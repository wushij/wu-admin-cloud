package com.wu.cloud.system.mapper;

import com.wu.cloud.system.entity.SysDashboardStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 工作台统计 Mapper
 *
 * <p>系统服务仅统计自身拥有的数据表（用户/角色/菜单/部门/岗位/登录日志），
 * 工单、审批等跨服务指标由 BFF 层调用对应微服务后聚合。</p>
 */
@Mapper
public interface SysDashboardMapper {

    /**
     * 单次查询聚合工作台计数类指标，避免多次 selectCount 往返
     *
     * @param todayStart     今日零点
     * @param yesterdayStart 昨日零点
     * @return 聚合统计行
     */
    @Select({
            "SELECT",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0) AS user_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 2) AS user_pending_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 0) AS user_disabled_count,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_time >= #{todayStart}) AS user_today,",
            "  (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND create_time >= #{yesterdayStart}",
            "     AND create_time < #{todayStart}) AS user_yesterday,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0) AS role_count,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0 AND create_time >= #{todayStart}) AS role_today,",
            "  (SELECT COUNT(*) FROM sys_role WHERE deleted = 0 AND create_time >= #{yesterdayStart}",
            "     AND create_time < #{todayStart}) AS role_yesterday,",
            "  (SELECT COUNT(*) FROM sys_menu WHERE deleted = 0) AS menu_count,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0) AS dept_count,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0 AND create_time >= #{todayStart}) AS dept_today,",
            "  (SELECT COUNT(*) FROM sys_dept WHERE deleted = 0 AND create_time >= #{yesterdayStart}",
            "     AND create_time < #{todayStart}) AS dept_yesterday,",
            "  (SELECT COUNT(*) FROM sys_post WHERE deleted = 0) AS post_count,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 0",
            "     AND login_time >= #{todayStart}) AS today_login_success,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 1",
            "     AND login_time >= #{todayStart}) AS today_login_fail,",
            "  (SELECT COUNT(*) FROM sys_login_log WHERE status = 0 AND login_time >= #{yesterdayStart}",
            "     AND login_time < #{todayStart}) AS yesterday_login_success"
    })
    SysDashboardStatsRow selectAggregateStats(@Param("todayStart") LocalDateTime todayStart,
                                              @Param("yesterdayStart") LocalDateTime yesterdayStart);
}
