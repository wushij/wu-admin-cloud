package com.wu.cloud.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.admin.service.DashboardService;
import com.wu.cloud.admin.vo.DashboardStatsVO;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin BFF 聚合 API 控制层
 */
@Tag(name = "工作台统计")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminBffController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 工作台统计聚合
     */
    @Operation(summary = "Redis 实时统计（图表）")
    @GetMapping("/stats")
    public R<DashboardStatsVO> getDashboardStats() {
        return R.ok(dashboardService.getDashboardStats(), "获取工作台统计成功");
    }

    /**
     * 工作台访问埋点
     * <p>C队修复：前端 recordVisit() 使用 GET（见 wu-admin-web/src/api/dashboard/index.ts），
     * 原 @Operation(summary = "上传工单附件")
 @PostMapping 与之不匹配会返回 405，改为 @Operation(summary = "多引擎文件流式读取与预览")
 @GetMapping 对齐。</p>
     */
    @Operation(summary = "记录工作台访问")
    @GetMapping("/visit")
    public R<Boolean> recordVisit() {
        dashboardService.recordVisit();
        return R.ok(true);
    }

    /**
     * 最近登录列表（独立查询，避免触发完整统计聚合）
     */
    @Operation(summary = "最近登录记录")
    @GetMapping("/recent-logins")
    public R<List<DashboardStatsVO.RecentLoginVO>> getRecentLogins(
            @RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit) {
        return R.ok(dashboardService.getRecentLogins(limit), "获取最近登录成功");
    }
}
