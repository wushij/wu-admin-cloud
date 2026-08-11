package com.wu.cloud.admin.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作台统计响应 VO（对齐前端 DashboardStats 字段）
 */
@Data
public class DashboardStatsVO implements Serializable {

    private Long userCount;
    private Long roleCount;
    private Long deptCount;
    private Long menuCount;
    private Long postCount;

    private Long onlineCount;
    private Long todayLoginSuccess;
    private Long todayLoginFail;
    private Long visitCount;
    private Long todayVisits;
    private Long yesterdayVisits;

    private String platformName;
    private String platformSubtitle;

    private Long ticketOpenCount;
    private Long ticketOverdueCount;
    private Long approvalPendingCount;
    private Long chatUnreadCount;

    private List<RecentLoginVO> recentLogins;

    @Data
    public static class RecentLoginVO implements Serializable {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private String ipaddr;
        private String loginLocation;
        private String loginTime;
        private String browser;
        private String os;
        private Integer status;
    }
}
