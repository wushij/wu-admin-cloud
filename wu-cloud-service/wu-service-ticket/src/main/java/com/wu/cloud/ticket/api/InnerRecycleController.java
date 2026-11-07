package com.wu.cloud.ticket.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工单服务回收站内部接口（跨服务回收站汇总与清理）
 * <p>供其他服务通过 OpenFeign 调用。路径 /inner/** 受网关 InnerApiGuardFilter 保护，禁止外部访问。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/recycle")
public class InnerRecycleController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取统计信息")
    @GetMapping("/count")
    public R<Map<String, Long>> count() {
        Map<String, Long> result = new HashMap<>(4);
        result.put("ticket", countDeleted("sys_ticket"));
        // 与回收列表一致：只统计审批单；审批记录不单独展示，计入会虚高
        result.put("approval", countDeleted("sys_approval_form"));
        result.put("notice", countDeleted("sys_notice"));
        return R.ok(result);
    }

    /**
     * 清理回收站中超过保留期的数据（物理删除），返回清理条数
     */
    @Operation(summary = "清理")
    @PostMapping("/purge")
    public R<Integer> purge(@RequestParam(value = "retentionDays", defaultValue = "30") Integer retentionDays) {
        int days = retentionDays == null || retentionDays <= 0 ? 30 : retentionDays;
        String deadline = LocalDateTime.now().minusDays(days).toString();
        int total = 0;
        // 回收站数据删除即物理清理，按 update_time 判定滞留时长
        for (String table : new String[]{"sys_ticket", "sys_approval_form", "sys_notice"}) {
            try {
                total += jdbcTemplate.update(
                        "DELETE FROM " + table + " WHERE deleted = 1 AND update_time < ?", deadline);
            } catch (Exception e) {
                // 单表失败不阻断其余表清理
            }
        }
        return R.ok(total);
    }

    private long countDeleted(String table) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE deleted = 1", Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
