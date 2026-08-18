package com.wu.cloud.infra.service.monitor.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.infra.dto.OnlineUserVO;
import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.security.TokenService;
import com.wu.cloud.infra.service.monitor.OnlineUserService;
import com.wu.cloud.common.utils.web.IpLocationUtils;
import com.wu.cloud.infra.util.UserAgentUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户服务实现（对齐单体 OnlineUserServiceImpl）
 */
@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_DETAIL_PREFIX = "monitor:online:detail:";
    private static final String ONLINE_ACTIVE_PREFIX = "dashboard:online:user:";
    private static final long TOUCH_THROTTLE_MS = 45_000L;

    private final ConcurrentHashMap<Long, Long> touchThrottleCache = new ConcurrentHashMap<>();

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private TokenService tokenService;
    @Resource
    private RemoteUserService remoteUserService;

    @Data
    public static class OnlineDetail {
        private Long userId;
        private String username;
        private String nickname;
        private String deptName;
        private String ipaddr;
        private String loginLocation;
        private String browser;
        private String os;
        private Long loginTime;
        private Long lastAccessTime;
    }

    @Override
    public void recordLoginSession(RecordLoginSessionDTO dto) {
        if (dto == null || dto.getUserId() == null) {
            return;
        }
        Long userId = dto.getUserId();
        String clientIp = dto.getClientIp() != null ? dto.getClientIp() : "";
        String userAgent = dto.getUserAgent() != null ? dto.getUserAgent() : "";

        OnlineDetail detail = new OnlineDetail();
        detail.setUserId(userId);
        detail.setUsername(dto.getUsername());
        detail.setNickname(dto.getNickname());
        detail.setIpaddr(clientIp);
        detail.setLoginLocation(IpLocationUtils.resolve(clientIp));
        detail.setBrowser(UserAgentUtils.parseBrowser(userAgent));
        detail.setOs(UserAgentUtils.parseOs(userAgent));
        long now = System.currentTimeMillis();
        detail.setLoginTime(now);
        detail.setLastAccessTime(now);

        enrichDeptAndAvatar(detail, userId);

        RBucket<String> bucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        bucket.set(JSONUtil.toJsonStr(detail), Duration.ofDays(1));

        refreshActiveMarker(userId, now);
        touchThrottleCache.put(userId, now);
    }

    @Override
    public void touchLastAccess(Long userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastTouch = touchThrottleCache.get(userId);
        if (lastTouch != null && now - lastTouch < TOUCH_THROTTLE_MS) {
            return;
        }
        touchThrottleCache.put(userId, now);
        refreshActiveMarker(userId, now);

        RBucket<String> bucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        String json = bucket.get();
        if (json != null) {
            OnlineDetail detail = JSONUtil.toBean(json, OnlineDetail.class);
            detail.setLastAccessTime(now);
            bucket.set(JSONUtil.toJsonStr(detail), Duration.ofDays(1));
        }
    }

    @Override
    public int countOnlineUsers() {
        // 轻量级统计：直接计数 Redis 活跃标记键，避免全量会话遍历 + 逐个 isLogin 校验
        // 超时导致 BFF 降级为 0 的根因就是 resolveActiveUserIds() 太重
        try {
            int count = countKeysByPattern(ONLINE_ACTIVE_PREFIX + "*");
            if (count > 0) {
                return count;
            }
        } catch (Exception e) {
            log.debug("[在线用户] 快速计数失败，回退到会话遍历: {}", e.getMessage());
        }
        return resolveActiveUserIds().size();
    }

    @Override
    public List<OnlineUserVO> listOnlineUsers() {
        List<OnlineUserVO> list = new ArrayList<>();
        for (Long userId : resolveActiveUserIds()) {
            try {
                if (!isUserActive(userId)) {
                    continue;
                }
                TokenService.LoginInfo loginInfo = tokenService.getLoginInfo(userId);
                if (loginInfo == null) {
                    continue;
                }
                if (redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).get() == null) {
                    touchLastAccess(userId);
                }
                list.add(buildVo(userId, loginInfo));
            } catch (Exception e) {
                log.warn("加载在线用户信息失败 userId={}", userId, e);
            }
        }
        list.sort((a, b) -> Long.compare(parseTime(b.getLastAccessTime()), parseTime(a.getLastAccessTime())));
        return list;
    }

    @Override
    public void forceLogout(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            StpUtil.kickout(userId);
        } catch (Exception ignored) {
        }
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {
        }
        tokenService.removeToken(userId);
        redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId).delete();
        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).delete();
        touchThrottleCache.remove(userId);
    }

    private boolean isUserActive(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            return StpUtil.isLogin(userId);
        } catch (Exception e) {
            return tokenService.getLoginInfo(userId) != null;
        }
    }

    private List<Long> resolveActiveUserIds() {
        Set<Long> ids = new LinkedHashSet<>(tokenService.listActiveUserIds());
        if (ids.isEmpty()) {
            collectUserIdsFromRedisSessions(ids);
        }
        List<Long> result = new ArrayList<>();
        for (Long userId : ids) {
            if (isUserActive(userId)) {
                result.add(userId);
            }
        }
        return result;
    }

    private void collectUserIdsFromRedisSessions(Set<Long> ids) {
        try {
            for (String pattern : new String[]{
                    "Authorization:login:session:*",
                    "Authorization:login:token:*"
            }) {
                for (String key : scanKeysByPattern(pattern)) {
                    Long userId = parseLoginIdFromSaTokenKey(key);
                    if (userId != null && isUserActive(userId)) {
                        ids.add(userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从 Redis 会话键解析在线用户失败", e);
        }
    }

    private Long parseLoginIdFromSaTokenKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        int idx = key.lastIndexOf(':');
        if (idx < 0 || idx >= key.length() - 1) {
            return null;
        }
        try {
            String tokenOrId = key.substring(idx + 1).trim();
            if (tokenOrId.matches("^\\d+$")) {
                return Long.parseLong(tokenOrId);
            }
            Object loginId = StpUtil.getLoginIdByToken(tokenOrId);
            if (loginId != null) {
                return Long.parseLong(loginId.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private OnlineUserVO buildVo(Long userId, TokenService.LoginInfo loginInfo) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setUserId(userId);
        vo.setTokenId(String.valueOf(userId));
        vo.setTokenValue(loginInfo.getToken());

        OnlineDetail detail = loadDetail(userId);
        SysUserDTO user = loadUser(userId);

        String loginName = detail != null && StringUtils.hasText(detail.getUsername())
                ? detail.getUsername()
                : (user != null ? user.getUsername() : loginInfo.getUsername());
        vo.setLoginName(loginName);

        String nickname = detail != null && StringUtils.hasText(detail.getNickname())
                ? detail.getNickname()
                : (user != null ? user.getNickname() : "");
        vo.setNickname(nickname);
        if (user != null && StringUtils.hasText(user.getAvatar())) {
            vo.setAvatar(user.getAvatar());
        }

        String deptName = detail != null && StringUtils.hasText(detail.getDeptName())
                ? detail.getDeptName()
                : (user != null && StringUtils.hasText(user.getDeptName()) ? user.getDeptName() : "");
        vo.setDeptName(StringUtils.hasText(deptName) ? deptName : nickname);

        if (detail != null) {
            vo.setIpaddr(detail.getIpaddr());
            vo.setLoginLocation(detail.getLoginLocation());
            vo.setBrowser(detail.getBrowser());
            vo.setOs(detail.getOs());
            vo.setLoginTime(formatTime(detail.getLoginTime() != null ? detail.getLoginTime() : loginInfo.getCreateTime()));
            vo.setLastAccessTime(formatTime(detail.getLastAccessTime() != null ? detail.getLastAccessTime() : detail.getLoginTime()));
        } else {
            vo.setIpaddr("");
            vo.setLoginLocation("");
            vo.setBrowser("");
            vo.setOs("");
            vo.setLoginTime(formatTime(loginInfo.getCreateTime()));
            vo.setLastAccessTime(formatTime(loginInfo.getCreateTime()));
        }
        vo.setStatus(1);
        return vo;
    }

    private void enrichDeptAndAvatar(OnlineDetail detail, Long userId) {
        SysUserDTO user = loadUser(userId);
        if (user == null) {
            return;
        }
        if (!StringUtils.hasText(detail.getNickname())) {
            detail.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getDeptName())) {
            detail.setDeptName(user.getDeptName());
        }
    }

    private SysUserDTO loadUser(Long userId) {
        try {
            R<SysUserDTO> res = remoteUserService.getUserById(userId);
            if (res != null && res.isSuccess()) {
                return res.getData();
            }
        } catch (Exception e) {
            log.debug("加载用户 {} 失败: {}", userId, e.getMessage());
        }
        return null;
    }

    private OnlineDetail loadDetail(Long userId) {
        RBucket<String> detailBucket = redissonClient.getBucket(ONLINE_DETAIL_PREFIX + userId);
        String json = detailBucket.get();
        if (json == null) {
            return null;
        }
        try {
            return JSONUtil.toBean(json, OnlineDetail.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTime(Long millis) {
        if (millis == null) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void refreshActiveMarker(Long userId, long timestamp) {
        long minutes = tokenService.tokenTimeoutMinutes();
        redissonClient.getBucket(ONLINE_ACTIVE_PREFIX + userId).set(timestamp, Duration.ofMinutes(minutes));
    }

    private long parseTime(String time) {
        if (!StringUtils.hasText(time)) {
            return 0L;
        }
        try {
            return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    private int countKeysByPattern(String pattern) {
        return (int) redissonClient.getKeys()
                .getKeysStream(scanOptions(pattern))
                .count();
    }

    private Iterable<String> scanKeysByPattern(String pattern) {
        return redissonClient.getKeys().getKeys(scanOptions(pattern));
    }

    private KeysScanOptions scanOptions(String pattern) {
        return KeysScanOptions.defaults().pattern(pattern).chunkSize(500);
    }
}
