package com.wu.cloud.admin.service.impl;

import com.wu.cloud.admin.service.DashboardService;
import com.wu.cloud.admin.vo.DashboardStatsVO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.api.infra.feign.RemoteJobService;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.message.feign.RemoteMessageService;
import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.api.system.feign.RemoteStatsService;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 工作台聚合（对齐单体：主统计优先 + 分钟级缓存；次要指标短超时降级，避免拖慢首屏）。
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 单路 RPC 硬超时：下游挂起时尽快降级，对齐单体「本地查库」量级体验 */
    private static final long RPC_TIMEOUT_MS = 800L;
    /** 整体等待上限：略大于单路超时，避免被最慢一路拖到 Feign 默认 3s */
    private static final long AGGREGATE_WAIT_MS = 1200L;
    /** 与单体 aggregate 缓存同量级（分钟级延迟可接受） */
    private static final long STATS_CACHE_SECONDS = 60L;
    private static final String STATS_CACHE_KEY = "dashboard:bff:stats:v1:";

    private static final int RECENT_LOGIN_LIMIT = 8;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RemoteStatsService remoteStatsService;
    private final RemoteTicketService remoteTicketService;
    private final RemoteOnlineUserService remoteOnlineUserService;
    private final RemoteFileService remoteFileService;
    private final RemoteMessageService remoteMessageService;
    private final RemoteJobService remoteJobService;
    private final StringRedisTemplate stringRedisTemplate;

    public DashboardServiceImpl(RemoteStatsService remoteStatsService,
                                RemoteTicketService remoteTicketService,
                                RemoteOnlineUserService remoteOnlineUserService,
                                RemoteFileService remoteFileService,
                                RemoteMessageService remoteMessageService,
                                RemoteJobService remoteJobService,
                                StringRedisTemplate stringRedisTemplate) {
        this.remoteStatsService = remoteStatsService;
        this.remoteTicketService = remoteTicketService;
        this.remoteOnlineUserService = remoteOnlineUserService;
        this.remoteFileService = remoteFileService;
        this.remoteMessageService = remoteMessageService;
        this.remoteJobService = remoteJobService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public DashboardStatsVO getDashboardStats() {
        Long userId = UserContextHolder.getUserId();
        // 心跳异步，不挡首屏
        if (userId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    remoteOnlineUserService.touchLastAccess(userId);
                } catch (Exception e) {
                    log.debug("[工作台] 在线心跳失败 userId={}", userId);
                }
            });
        }

        String cacheKey = STATS_CACHE_KEY + (userId == null ? "anon" : userId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                DashboardStatsVO hit = cn.hutool.json.JSONUtil.toBean(cached, DashboardStatsVO.class);
                if (hit != null) {
                    return hit;
                }
            }
        } catch (Exception e) {
            log.debug("[工作台] 读缓存失败，走实时聚合: {}", e.getMessage());
        }

        // 主路径：系统聚合（含用户数/访问/配置分组等）
        CompletableFuture<Map<String, Object>> statsFuture = supplyWithTimeout(
                () -> callOrDefault(() -> remoteStatsService.getDashboardStats(), Collections.emptyMap(), "系统统计"),
                Collections.emptyMap());

        // 次要路径：短超时，失败即 0（不拖主统计）
        CompletableFuture<Integer> onlineFuture = supplyWithTimeout(
                () -> callOrDefault(() -> remoteOnlineUserService.countOnlineUsers(), 0, "在线用户"), 0);
        CompletableFuture<TicketDashboardDTO> ticketStatsFuture = supplyWithTimeout(
                () -> callOrDefault(() -> remoteTicketService.getDashboardStats(userId), new TicketDashboardDTO(), "工单统计"),
                new TicketDashboardDTO());
        CompletableFuture<Long> chatUnreadFuture = supplyWithTimeout(
                () -> userId == null ? 0L : callOrDefault(
                        () -> remoteMessageService.getChatUnreadCount(userId), 0L, "IM未读"), 0L);
        CompletableFuture<Long> fileCountFuture = supplyWithTimeout(
                () -> callOrDefault(() -> remoteFileService.countFiles(), 0L, "文件数量"), 0L);
        CompletableFuture<Map<String, Object>> jobFuture = supplyWithTimeout(
                () -> callOrDefault(() -> remoteJobService.overview(), Collections.emptyMap(), "定时任务"),
                Collections.emptyMap());

        try {
            CompletableFuture.allOf(statsFuture, onlineFuture, ticketStatsFuture,
                            chatUnreadFuture, fileCountFuture, jobFuture)
                    .get(AGGREGATE_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            log.debug("[工作台] 聚合等待超时，已用已完成结果降级");
        } catch (Exception e) {
            log.warn("[工作台] 聚合子任务异常，已降级", e);
        }

        DashboardStatsVO vo = new DashboardStatsVO();
        Map<String, Object> stats = getNowOrDefault(statsFuture, Collections.emptyMap());
        applySiteFromStats(vo, stats);
        vo.setUserCount(toLong(stats.get("userCount")));
        vo.setRoleCount(toLong(stats.get("roleCount")));
        vo.setMenuCount(toLong(stats.get("menuCount")));
        vo.setDeptCount(toLong(stats.get("deptCount")));
        vo.setPostCount(toLong(stats.get("postCount")));
        vo.setTodayLoginSuccess(toLong(stats.get("todayLoginSuccess")));
        vo.setTodayLoginFail(toLong(stats.get("todayLoginFail")));
        vo.setTodayVisits(toLong(stats.get("todayVisits")));
        vo.setYesterdayVisits(toLong(stats.get("yesterdayVisits")));
        vo.setVisitCount(nullToZero(vo.getTodayVisits()) + nullToZero(vo.getYesterdayVisits()));
        vo.setConfigGroupCount(toLong(stats.get("configGroupCount")));
        vo.setOnlineCount((long) getNowOrDefault(onlineFuture, 0));
        vo.setChatUnreadCount(getNowOrDefault(chatUnreadFuture, 0L));
        vo.setFileCount(getNowOrDefault(fileCountFuture, 0L));

        Map<String, Object> job = getNowOrDefault(jobFuture, Collections.emptyMap());
        vo.setJobTotalCount(toLong(firstNonNull(job.get("totalJobs"), job.get("jobTotalCount"))));
        vo.setJobRunningCount(toLong(firstNonNull(job.get("runningJobs"), job.get("jobRunningCount"))));
        vo.setJobPausedCount(toLong(firstNonNull(job.get("pausedJobs"), job.get("jobPausedCount"))));

        TicketDashboardDTO ticketStats = getNowOrDefault(ticketStatsFuture, new TicketDashboardDTO());
        vo.setTicketOpenCount(ticketStats == null ? 0L : nullToZero(ticketStats.getTicketOpenCount()));
        vo.setTicketOverdueCount(ticketStats == null ? 0L : nullToZero(ticketStats.getTicketOverdueCount()));
        vo.setApprovalPendingCount(ticketStats == null ? 0L : nullToZero(ticketStats.getApprovalPendingCount()));

        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    cn.hutool.json.JSONUtil.toJsonStr(vo), STATS_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("[工作台] 写缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public List<DashboardStatsVO.RecentLoginVO> getRecentLogins(Integer limit) {
        int size = (limit == null || limit <= 0) ? RECENT_LOGIN_LIMIT : limit;
        List<RecentLoginDTO> logins = callOrDefault(
                () -> remoteStatsService.getRecentLogins(size), Collections.emptyList(), "最近登录");
        return convertRecentLogins(logins);
    }

    @Override
    public void recordVisit() {
        try {
            remoteStatsService.recordVisit();
        } catch (Exception e) {
            log.warn("[工作台] 访问埋点上报失败", e);
        }
    }

    private <T> CompletableFuture<T> supplyWithTimeout(Supplier<T> supplier, T defaultValue) {
        return CompletableFuture.supplyAsync(supplier)
                .orTimeout(RPC_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    if (!(ex.getCause() instanceof TimeoutException)
                            && !(ex instanceof TimeoutException)) {
                        log.debug("[工作台] 子任务失败已降级: {}", ex.toString());
                    }
                    return defaultValue;
                });
    }

    private void applySiteFromStats(DashboardStatsVO vo, Map<String, Object> stats) {
        Object name = stats.get("platformName");
        Object subtitle = stats.get("platformSubtitle");
        vo.setPlatformName(name != null && !String.valueOf(name).isBlank()
                ? String.valueOf(name) : "Admin Platform");
        vo.setPlatformSubtitle(subtitle != null && !String.valueOf(subtitle).isBlank()
                ? String.valueOf(subtitle) : "统一运维 · 高效管控");
    }

    private List<DashboardStatsVO.RecentLoginVO> convertRecentLogins(List<RecentLoginDTO> logins) {
        if (logins == null || logins.isEmpty()) {
            return Collections.emptyList();
        }
        List<DashboardStatsVO.RecentLoginVO> result = new ArrayList<>(logins.size());
        for (RecentLoginDTO dto : logins) {
            DashboardStatsVO.RecentLoginVO item = new DashboardStatsVO.RecentLoginVO();
            item.setUserId(dto.getUserId());
            item.setUsername(dto.getUsername());
            item.setNickname(dto.getNickname());
            item.setAvatar(dto.getAvatar());
            item.setIpaddr(dto.getIpaddr());
            item.setLoginLocation(dto.getLoginLocation());
            item.setBrowser(dto.getBrowser());
            item.setOs(dto.getOs());
            item.setStatus(dto.getStatus());
            item.setLoginTime(dto.getLoginTime() == null ? null : dto.getLoginTime().format(TIME_FORMATTER));
            result.add(item);
        }
        return result;
    }

    private <T> T callOrDefault(Supplier<R<T>> supplier, T defaultValue, String scene) {
        try {
            R<T> response = supplier.get();
            if (response == null || response.getData() == null) {
                return defaultValue;
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("[工作台] {} 数据获取失败，已降级为默认值", scene);
            return defaultValue;
        }
    }

    private <T> T getNowOrDefault(CompletableFuture<T> future, T defaultValue) {
        T value = future.getNow(null);
        return value == null ? defaultValue : value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }
}
