package com.wu.cloud.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.service.SysStatsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内部 RPC 端点：系统数据统计（仅供微服务内部 OpenFeign 调用，由网关 403 黑名单保护）
 */
@Hidden
@RestController
@RequestMapping("/inner/stats")
public class InnerStatsController {

    @Resource
    private SysStatsService sysStatsService;

    @Operation(summary = "获取统计信息")
    @GetMapping("/count")
    public R<Map<String, Long>> countAll() {
        return R.ok(sysStatsService.countAll());
    }

    @Operation(summary = "获取详情")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboardStats() {
        return R.ok(sysStatsService.getDashboardStats());
    }

    @Operation(summary = "最近登录记录")
    @GetMapping("/recent-logins")
    public R<List<RecentLoginDTO>> getRecentLogins(
            @RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit) {
        return R.ok(sysStatsService.getRecentLogins(limit == null ? 8 : limit));
    }

    @Operation(summary = "新增")
    @PostMapping("/visit")
    public R<Boolean> recordVisit() {
        sysStatsService.recordVisit();
        return R.ok(true);
    }

    /**
     * 近 N 天（1~30）登录成功/失败次数统计（AI 登录统计工具等场景）
     */
    @Operation(summary = "获取统计信息")
    @GetMapping("/login-count")
    public R<Map<String, Long>> countLoginByStartTime(
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        return R.ok(sysStatsService.countLoginSince(days == null ? 7 : days));
    }
}
