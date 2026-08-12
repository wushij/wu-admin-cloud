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

    /** 文件数量（对齐单体 fileCount） */
    private Long fileCount;
    /** 定时任务总数 */
    private Long jobTotalCount;
    private Long jobRunningCount;
    private Long jobPausedCount;
    /** 系统配置分组数 */
    private Long configGroupCount;

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
