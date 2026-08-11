package com.wu.cloud.system.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作台聚合统计查询结果行
 */
@Data
public class SysDashboardStatsRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userCount;
    private Long userPendingCount;
    private Long userDisabledCount;
    private Long userToday;
    private Long userYesterday;

    private Long roleCount;
    private Long roleToday;
    private Long roleYesterday;

    private Long menuCount;

    private Long deptCount;
    private Long deptToday;
    private Long deptYesterday;

    private Long postCount;

    private Long todayLoginSuccess;
    private Long todayLoginFail;
    private Long yesterdayLoginSuccess;
}
