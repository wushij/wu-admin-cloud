package com.wu.cloud.admin.service;

import com.wu.cloud.admin.vo.DashboardStatsVO;

import java.util.List;

/**
 * 工作台聚合服务：并行聚合系统、工单等微服务的统计数据，任一下游不可用时降级。
 */
public interface DashboardService {

    /**
     * 聚合工作台统计数据
     */
    DashboardStatsVO getDashboardStats();

    /**
     * 单独获取最近登录列表
     *
     * @param limit 返回条数，null 或 <=0 时取默认值
     */
    List<DashboardStatsVO.RecentLoginVO> getRecentLogins(Integer limit);

    /**
     * 记录一次工作台访问
     */
    void recordVisit();
}
