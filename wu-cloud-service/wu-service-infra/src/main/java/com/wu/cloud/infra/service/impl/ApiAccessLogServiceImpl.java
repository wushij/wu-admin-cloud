package com.wu.cloud.infra.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.ApiAccessLogDO;
import com.wu.cloud.infra.mapper.ApiAccessLogMapper;
import com.wu.cloud.infra.service.ApiAccessLogService;
import com.wu.cloud.infra.service.UserNameResolver;
import com.wu.cloud.infra.service.vo.ApiAccessDailyStatVO;
import com.wu.cloud.infra.service.vo.ApiAccessMethodStatVO;
import com.wu.cloud.infra.service.vo.ApiAccessSummaryVO;
import com.wu.cloud.infra.service.vo.ApiAccessUserStatVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 访问统计日志服务实现
 */
@Service
public class ApiAccessLogServiceImpl implements ApiAccessLogService {

    @Resource
    private ApiAccessLogMapper apiAccessLogMapper;
    @Resource
    private UserNameResolver userNameResolver;

    @Override
    public PageResult<ApiAccessLogDO> page(Integer pageNo, Integer pageSize, Long userId,
                                           String apiPath, String method, Integer success,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        int current = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int size = pageSize == null || pageSize <= 0 ? 20 : pageSize;
        LambdaQueryWrapper<ApiAccessLogDO> wrapper = new LambdaQueryWrapper<ApiAccessLogDO>()
                .eq(userId != null, ApiAccessLogDO::getUserId, userId)
                .like(apiPath != null && !apiPath.isBlank(), ApiAccessLogDO::getApiPath, apiPath)
                .eq(method != null && !method.isBlank(), ApiAccessLogDO::getMethod, method)
                .eq(success != null, ApiAccessLogDO::getSuccess, success)
                .ge(startTime != null, ApiAccessLogDO::getStartTime, startTime)
                .le(endTime != null, ApiAccessLogDO::getStartTime, endTime)
                .orderByDesc(ApiAccessLogDO::getStartTime);
        Page<ApiAccessLogDO> page = apiAccessLogMapper.selectPage(new Page<>(current, size), wrapper);
        userNameResolver.fillApiAccessUsernames(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Override
    public Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDate startDate = startTime != null ? startTime.toLocalDate() : LocalDate.now().minusDays(6);
        LocalDate endDate = endTime != null ? endTime.toLocalDate() : LocalDate.now();
        LocalDateTime sqlStart = startDate.atStartOfDay();
        LocalDateTime sqlEnd = endDate.plusDays(1).atStartOfDay();

        ApiAccessSummaryVO summary = apiAccessLogMapper.selectSummary(sqlStart, sqlEnd);
        long total = summary != null && summary.getTotalCount() != null ? summary.getTotalCount() : 0L;
        long successCount = summary != null && summary.getSuccessCount() != null ? summary.getSuccessCount() : 0L;
        long failCount = total - successCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", total);
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        Map<String, Map<String, Object>> dailyStats = new LinkedHashMap<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 0L);
            empty.put("success", 0L);
            empty.put("fail", 0L);
            dailyStats.put(day.toString(), empty);
        }
        for (ApiAccessDailyStatVO row : apiAccessLogMapper.selectDailyStats(sqlStart, sqlEnd)) {
            if (row.getStatDate() == null) {
                continue;
            }
            Map<String, Object> dayMap = dailyStats.get(row.getStatDate());
            if (dayMap == null) {
                continue;
            }
            long dayTotal = row.getTotal() != null ? row.getTotal() : 0L;
            long daySuccess = row.getSuccessCount() != null ? row.getSuccessCount() : 0L;
            dayMap.put("total", dayTotal);
            dayMap.put("success", daySuccess);
            dayMap.put("fail", dayTotal - daySuccess);
        }
        result.put("dailyStats", dailyStats);

        List<Map<String, Object>> topPaths = apiAccessLogMapper.selectTopPaths(sqlStart, sqlEnd, 10).stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("apiPath", row.getApiPath());
                    m.put("count", row.getCount() != null ? row.getCount() : 0L);
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topPaths", topPaths);

        Map<String, Long> methodCount = new HashMap<>();
        for (ApiAccessMethodStatVO row : apiAccessLogMapper.selectMethodCounts(sqlStart, sqlEnd)) {
            methodCount.put(row.getMethod(), row.getCount() != null ? row.getCount() : 0L);
        }
        result.put("methodCount", methodCount);

        List<ApiAccessUserStatVO> userRows = apiAccessLogMapper.selectTopUsers(sqlStart, sqlEnd, 10);
        Map<Long, String> userNames = userNameResolver.batchResolve(
                userRows.stream().map(ApiAccessUserStatVO::getUserId).collect(Collectors.toSet()));
        List<Map<String, Object>> topUsers = new ArrayList<>();
        for (ApiAccessUserStatVO row : userRows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", row.getUserId());
            m.put("count", row.getCount() != null ? row.getCount() : 0L);
            String username = userNames.get(row.getUserId());
            m.put("username", username != null ? username : "用户" + row.getUserId());
            topUsers.add(m);
        }
        result.put("topUsers", topUsers);

        return result;
    }
}
