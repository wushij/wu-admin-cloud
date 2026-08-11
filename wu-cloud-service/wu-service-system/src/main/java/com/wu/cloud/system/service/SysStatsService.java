package com.wu.cloud.system.service;

import com.wu.cloud.api.system.dto.RecentLoginDTO;

import java.util.List;
import java.util.Map;

/**
 * 系统数据统计服务（供 BFF 聚合工作台使用）
 */
public interface SysStatsService {

    /**
     * 统计各维度数量：userCount / roleCount / menuCount / deptCount / postCount
     */
    Map<String, Long> countAll();

    /**
     * 工作台聚合统计：在 {@link #countAll()} 基础上补充今日/昨日新增、登录成功失败数与环比趋势。
     *
     * <p>结果带 Redis 短时缓存，避免工作台频繁刷新压垮数据库。</p>
     *
     * @return 统计指标键值对
     */
    Map<String, Object> getDashboardStats();

    /**
     * 最近登录记录（按登录时间倒序），并回填用户昵称与头像
     *
     * @param limit 返回条数，超出范围时自动收敛到 1~50
     * @return 最近登录列表
     */
    List<RecentLoginDTO> getRecentLogins(int limit);

    /**
     * 记录一次工作台访问，用于统计今日/昨日访问量
     */
    void recordVisit();
}
