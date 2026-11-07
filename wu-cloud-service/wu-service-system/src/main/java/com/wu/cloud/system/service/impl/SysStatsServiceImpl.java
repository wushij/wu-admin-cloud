package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.entity.SysDashboardStatsRow;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysPostDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.DeptMapper;
import com.wu.cloud.system.mapper.MenuMapper;
import com.wu.cloud.system.mapper.PostMapper;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysConfigGroupMapper;
import com.wu.cloud.system.mapper.SysDashboardMapper;
import com.wu.cloud.system.mapper.SysLoginLogMapper;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.service.SysStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统数据统计服务实现
 */
@Slf4j
@Service
public class SysStatsServiceImpl implements SysStatsService {

    /** 每日访问量计数键前缀 */
    private static final String VISIT_COUNT_KEY = "dashboard:visit:date:";

    /** 访问量计数保留 7 天 */
    private static final Duration VISIT_COUNT_TTL = Duration.ofDays(7);

    /** 最近登录默认与最大条数 */
    private static final int RECENT_LOGIN_DEFAULT = 8;
    private static final int RECENT_LOGIN_MAX = 50;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final DeptMapper deptMapper;
    private final PostMapper postMapper;
    private final SysDashboardMapper sysDashboardMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysConfigGroupMapper sysConfigGroupMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DynamicConfigHelper dynamicConfigHelper;

    public SysStatsServiceImpl(SysUserMapper sysUserMapper, RoleMapper roleMapper,
                               MenuMapper menuMapper, DeptMapper deptMapper, PostMapper postMapper,
                               SysDashboardMapper sysDashboardMapper, SysLoginLogMapper sysLoginLogMapper,
                               SysConfigGroupMapper sysConfigGroupMapper,
                               StringRedisTemplate stringRedisTemplate,
                               DynamicConfigHelper dynamicConfigHelper) {
        this.sysUserMapper = sysUserMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.deptMapper = deptMapper;
        this.postMapper = postMapper;
        this.sysDashboardMapper = sysDashboardMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.sysConfigGroupMapper = sysConfigGroupMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.dynamicConfigHelper = dynamicConfigHelper;
    }

    @Override
    public Map<String, Long> countAll() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("userCount", sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()));
        result.put("roleCount", roleMapper.selectCount(new LambdaQueryWrapper<SysRoleDO>()));
        result.put("menuCount", menuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()));
        result.put("deptCount", deptMapper.selectCount(new LambdaQueryWrapper<SysDeptDO>()));
        result.put("postCount", postMapper.selectCount(new LambdaQueryWrapper<SysPostDO>()));
        return result;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        Map<String, Object> stats = new LinkedHashMap<>();

        SysDashboardStatsRow row;
        try {
            row = sysDashboardMapper.selectAggregateStats(todayStart, yesterdayStart);
        } catch (Exception e) {
            // 聚合 SQL 失败时降级为逐表 count，保证工作台仍可展示
            log.warn("[工作台] 聚合统计查询失败，降级为逐表统计", e);
            countAll().forEach(stats::put);
            stats.put("todayVisits", getDayVisitCount(today));
            stats.put("yesterdayVisits", getDayVisitCount(today.minusDays(1)));
            stats.put("configGroupCount", sysConfigGroupMapper.selectCount(new LambdaQueryWrapper<SysConfigGroupDO>()));
            dynamicConfigHelper.fillDashboardConfigFields(stats);
            return stats;
        }

        if (row == null) {
            row = new SysDashboardStatsRow();
        }

        stats.put("userCount", nullSafe(row.getUserCount()));
        stats.put("roleCount", nullSafe(row.getRoleCount()));
        stats.put("menuCount", nullSafe(row.getMenuCount()));
        stats.put("deptCount", nullSafe(row.getDeptCount()));
        stats.put("postCount", nullSafe(row.getPostCount()));

        stats.put("userPendingCount", nullSafe(row.getUserPendingCount()));
        stats.put("userDisabledCount", nullSafe(row.getUserDisabledCount()));

        stats.put("todayLoginSuccess", nullSafe(row.getTodayLoginSuccess()));
        stats.put("todayLoginFail", nullSafe(row.getTodayLoginFail()));
        stats.put("yesterdayLoginSuccess", nullSafe(row.getYesterdayLoginSuccess()));

        // 环比趋势：今日新增相对昨日新增的百分比变化
        stats.put("userTrend", trendPercent(nullSafe(row.getUserToday()), nullSafe(row.getUserYesterday())));
        stats.put("roleTrend", trendPercent(nullSafe(row.getRoleToday()), nullSafe(row.getRoleYesterday())));
        stats.put("deptTrend", trendPercent(nullSafe(row.getDeptToday()), nullSafe(row.getDeptYesterday())));
        stats.put("menuTrend", 0);

        stats.put("todayVisits", getDayVisitCount(today));
        stats.put("yesterdayVisits", getDayVisitCount(today.minusDays(1)));
        stats.put("configGroupCount", sysConfigGroupMapper.selectCount(new LambdaQueryWrapper<SysConfigGroupDO>()));
        dynamicConfigHelper.fillDashboardConfigFields(stats);
        return stats;
    }

    @Override
    public List<RecentLoginDTO> getRecentLogins(int limit) {
        int size = limit <= 0 ? RECENT_LOGIN_DEFAULT : Math.min(limit, RECENT_LOGIN_MAX);

        List<SysLoginLogDO> logs = sysLoginLogMapper.selectList(
                new LambdaQueryWrapper<SysLoginLogDO>()
                        .orderByDesc(SysLoginLogDO::getLoginTime)
                        .last("LIMIT " + size));
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量回填昵称与头像，避免 N+1 查询
        Set<Long> userIds = logs.stream()
                .map(SysLoginLogDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUserDO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUserDO::getId, Function.identity(), (a, b) -> a));

        List<RecentLoginDTO> result = new ArrayList<>(logs.size());
        for (SysLoginLogDO logDO : logs) {
            RecentLoginDTO dto = new RecentLoginDTO();
            dto.setUserId(logDO.getUserId());
            dto.setUsername(logDO.getUsername());
            dto.setIpaddr(logDO.getIp());
            dto.setLoginLocation(logDO.getAddress());
            dto.setBrowser(logDO.getBrowser());
            dto.setOs(logDO.getOs());
            dto.setStatus(logDO.getStatus());
            dto.setLoginTime(logDO.getLoginTime());

            SysUserDO user = logDO.getUserId() == null ? null : userMap.get(logDO.getUserId());
            if (user != null) {
                dto.setNickname(user.getNickname());
                dto.setAvatar(user.getAvatar());
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public void recordVisit() {
        String key = VISIT_COUNT_KEY + LocalDate.now().format(DATE_FORMATTER);
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key);
            // 仅在首次创建时设置过期时间，避免每次访问都重置 TTL
            if (value != null && value == 1L) {
                stringRedisTemplate.expire(key, VISIT_COUNT_TTL);
            }
        } catch (Exception e) {
            // 访问埋点属于弱一致性统计，Redis 异常不应影响主流程
            log.warn("[工作台] 记录访问量失败 key={}", key, e);
        }
    }

    @Override
    public Map<String, Long> countLoginSince(int days) {
        int validDays = Math.max(1, Math.min(days, 30));
        LocalDateTime since = LocalDateTime.now().minusDays(validDays);
        Long success = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLogDO>()
                .ge(SysLoginLogDO::getLoginTime, since)
                .eq(SysLoginLogDO::getStatus, 0));
        Long fail = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLogDO>()
                .ge(SysLoginLogDO::getLoginTime, since)
                .eq(SysLoginLogDO::getStatus, 1));
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("successCount", nullSafe(success));
        result.put("failCount", nullSafe(fail));
        return result;
    }

    /**
     * 读取指定日期的访问量
     */
    private long getDayVisitCount(LocalDate date) {
        String key = VISIT_COUNT_KEY + date.format(DATE_FORMATTER);
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("[工作台] 读取访问量失败 key={}", key, e);
            return 0L;
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 计算环比百分比
     *
     * @param todayNew     今日新增
     * @param yesterdayNew 昨日新增
     * @return 百分比整数，昨日为 0 时今日有新增返回 100，否则返回 0
     */
    private int trendPercent(long todayNew, long yesterdayNew) {
        if (yesterdayNew <= 0) {
            return todayNew > 0 ? 100 : 0;
        }
        return (int) Math.round((todayNew - yesterdayNew) * 100.0 / yesterdayNew);
    }
}

