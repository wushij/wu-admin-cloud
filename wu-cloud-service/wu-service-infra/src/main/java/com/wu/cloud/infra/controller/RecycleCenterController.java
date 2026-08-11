package com.wu.cloud.infra.controller;

import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 回收站中心 REST 控制器（P0 修复：跨服务聚合，禁止直查 wu_system/wu_ticket 表）
 *
 * <p>此前该控制器直接使用 {@code wu_infra} 数据源查询 {@code sys_user/sys_role/sys_notice}
 * 等不属于本服务的表，运行时必抛 {@code Table doesn't exist}。现改为通过
 * {@code @LoadBalanced RestTemplate} 调用各业务服务自己的 {@code /inner/recycle/count}
 * 聚合回收站计数，不再跨库直连。</p>
 *
 * <p>系统表（user/role/menu/dept/post/dictType/dictData/configGroup）由 wu-service-system
 * 提供；ticket/approval/notice 由 wu-service-ticket；announce 由 wu-service-message；
 * file/gen 由 wu-service-file；job/jobLog 由本服务（wu_infra）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/recycle")
public class RecycleCenterController {

    /** 受回收站管理的表（表名 -> 展示名称） */
    private static final Map<String, String> MANAGED_TABLES = Map.ofEntries(
            Map.entry("sys_user", "用户"),
            Map.entry("sys_role", "角色"),
            Map.entry("sys_dept", "部门"),
            Map.entry("sys_menu", "菜单"),
            Map.entry("sys_post", "岗位"),
            Map.entry("sys_notice", "通知公告"),
            Map.entry("sys_config_group", "系统配置分组")
    );

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/tables")
    public R<Map<String, String>> tables() {
        return R.ok(MANAGED_TABLES);
    }

    /**
     * 跨服务聚合回收站计数。汇总各服务 {@code /inner/recycle/count} 的返回值，
     * 某服务不可用/超时时该项为 0，不影响整体响应。
     */
    @GetMapping("/summary")
    public R<Map<String, Long>> summary() {
        Map<String, Long> result = new LinkedHashMap<>(16);
        // 本服务（wu_infra）本地计数
        Map<String, Long> local = fetch("wu-service-infra");
        result.put("job", value(local, "job"));
        result.put("jobLog", value(local, "jobLog"));
        // system 服务（wu_system 库）
        Map<String, Long> system = fetch("wu-service-system");
        result.put("user", value(system, "user"));
        result.put("role", value(system, "role"));
        result.put("menu", value(system, "menu"));
        result.put("dept", value(system, "dept"));
        result.put("post", value(system, "post"));
        result.put("dictType", value(system, "dictType"));
        result.put("dictData", value(system, "dictData"));
        result.put("configGroup", value(system, "configGroup"));
        // ticket 服务
        Map<String, Long> ticket = fetch("wu-service-ticket");
        result.put("ticket", value(ticket, "ticket"));
        result.put("approval", value(ticket, "approval"));
        result.put("notice", value(ticket, "notice"));
        // message 服务
        Map<String, Long> message = fetch("wu-service-message");
        result.put("announce", value(message, "announce"));
        // file 服务
        Map<String, Long> file = fetch("wu-service-file");
        result.put("file", value(file, "file"));
        result.put("gen", value(file, "gen"));

        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);
        return R.ok(result);
    }

    /**
     * 调用目标服务 /inner/recycle/count 获取计数 Map；失败返回空 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> fetch(String serviceName) {
        try {
            String url = "http://" + serviceName + "/inner/recycle/count";
            R<Map<String, Object>> resp = restTemplate.getForObject(url, R.class);
            if (resp != null && resp.getCode() == 200 && resp.getData() != null) {
                return toLongMap(resp.getData());
            }
        } catch (Exception e) {
            log.warn("[RecycleCenter] 跨服务回收站计数失败 service={} err={}", serviceName, e.getMessage());
        }
        return Map.of();
    }

    /** RestTemplate 反序列化 JSON 数字常为 Integer，统一转为 Long */
    private Map<String, Long> toLongMap(Map<String, Object> raw) {
        Map<String, Long> result = new LinkedHashMap<>(raw.size());
        raw.forEach((k, v) -> {
            if (v instanceof Number number) {
                result.put(k, number.longValue());
            }
        });
        return result;
    }

    private long value(Map<String, Long> map, String key) {
        Long v = map.get(key);
        return v != null ? v : 0L;
    }
}
