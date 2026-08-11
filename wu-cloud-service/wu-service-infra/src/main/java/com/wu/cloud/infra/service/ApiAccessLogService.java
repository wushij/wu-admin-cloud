package com.wu.cloud.infra.service;

import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.ApiAccessLogDO;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 访问统计日志服务接口
 */
public interface ApiAccessLogService {

    /**
     * 分页查询 API 访问日志
     */
    PageResult<ApiAccessLogDO> page(Integer pageNo, Integer pageSize, Long userId,
                                    String apiPath, String method, Integer success,
                                    LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定日期范围内的访问量（总次数、成功/失败、每日趋势、TOP 路径、TOP 用户、方法分布）
     */
    Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime);
}
