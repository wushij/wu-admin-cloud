package com.wu.cloud.system.controller;

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
@RestController
@RequestMapping("/inner/stats")
public class InnerStatsController {

    @Resource
    private SysStatsService sysStatsService;

    @GetMapping("/count")
    public R<Map<String, Long>> countAll() {
        return R.ok(sysStatsService.countAll());
    }

    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboardStats() {
        return R.ok(sysStatsService.getDashboardStats());
    }

    @GetMapping("/recent-logins")
    public R<List<RecentLoginDTO>> getRecentLogins(
            @RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit) {
        return R.ok(sysStatsService.getRecentLogins(limit == null ? 8 : limit));
    }

    @PostMapping("/visit")
    public R<Boolean> recordVisit() {
        sysStatsService.recordVisit();
        return R.ok(true);
    }
}
