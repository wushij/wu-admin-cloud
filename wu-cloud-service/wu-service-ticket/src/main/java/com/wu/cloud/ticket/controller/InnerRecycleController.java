package com.wu.cloud.ticket.controller;

import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 工单服务回收站计数内部接口（B 队 P1 修复：跨服务回收站汇总）
 * <p>供 wu-service-system 的 RecycleController 通过 @LoadBalanced RestTemplate 调用。
 * 路径 /inner/** 受网关 InnerApiGuardFilter 保护，禁止外部访问。</p>
 */
@RestController
@RequestMapping("/inner/recycle")
public class InnerRecycleController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/count")
    public R<Map<String, Long>> count() {
        Map<String, Long> result = new HashMap<>(4);
        result.put("ticket", countDeleted("sys_ticket"));
        result.put("approval", countDeleted("sys_approval_form") + countDeleted("sys_approval_record"));
        result.put("notice", countDeleted("sys_notice"));
        return R.ok(result);
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
