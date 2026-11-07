package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.api.infra.feign.recycle.RecycleCountEndpoint;
import com.wu.cloud.api.infra.feign.recycle.RemoteFileRecycleClient;
import com.wu.cloud.api.infra.feign.recycle.RemoteInfraRecycleClient;
import com.wu.cloud.api.infra.feign.recycle.RemoteMessageRecycleClient;
import com.wu.cloud.api.infra.feign.recycle.RemoteTicketRecycleClient;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.mapper.SysRecycleMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 回收站汇总统计 REST 控制器（全站唯一聚合口径，前端 /system/recycle/summary）
 *
 * <p>本地统计 system 库内表的回收站计数；ticket/message/infra/file 维度
 * 通过类型化 OpenFeign 契约调用各服务自己的 /inner/recycle/count 端点。
 * 某服务不可用或超时时返回 0，不影响整体响应。</p>
 */
@Slf4j
@Tag(name = "回收中心")
@RestController
@RequestMapping("/api/system/recycle")
public class RecycleController {

    @Resource
    private SysRecycleMapper sysRecycleMapper;
    @Resource
    private RemoteTicketRecycleClient ticketRecycleClient;
    @Resource
    private RemoteMessageRecycleClient messageRecycleClient;
    @Resource
    private RemoteInfraRecycleClient infraRecycleClient;
    @Resource
    private RemoteFileRecycleClient fileRecycleClient;

    @RequiresPermissions("system:recycle:query")
    @Operation(summary = "回收站汇总统计")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        // ===== 本地（system 库）回收站计数 =====
        long user = sysRecycleMapper.countDeletedUsers(null, null, null, null, null);
        long role = sysRecycleMapper.countDeletedRoles(null);
        long menu = sysRecycleMapper.countDeletedMenus(null);
        long dept = sysRecycleMapper.countDeletedDepts(null);
        long post = sysRecycleMapper.countDeletedPosts();
        long dict = sysRecycleMapper.countDeletedDictTypes(null);
        long dictData = sysRecycleMapper.countDeletedDictData(null, null);

        // ===== 跨服务回收站计数（OpenFeign 契约调用，失败返回 0）=====
        Map<String, Object> ticketCounts = fetch(ticketRecycleClient);
        Map<String, Object> messageCounts = fetch(messageRecycleClient);
        Map<String, Object> infraCounts = fetch(infraRecycleClient);
        Map<String, Object> fileCounts = fetch(fileRecycleClient);

        long ticket = toLong(ticketCounts.get("ticket"));
        long approval = toLong(ticketCounts.get("approval"));
        long notice = toLong(ticketCounts.get("notice"));
        long announce = toLong(messageCounts.get("announce"));
        long job = toLong(infraCounts.get("job"));
        long jobLog = toLong(infraCounts.get("jobLog"));
        long file = toLong(fileCounts.get("file"));
        long gen = toLong(infraCounts.get("gen"));

        long total = user + role + menu + dept + post + dict + dictData
                + ticket + approval + notice + announce + job + jobLog + file + gen;

        Map<String, Object> result = new HashMap<>(16);
        result.put("user", user);
        result.put("role", role);
        result.put("menu", menu);
        result.put("dept", dept);
        result.put("post", post);
        result.put("ticket", ticket);
        result.put("approval", approval);
        result.put("notice", notice);
        result.put("dict", dict);
        result.put("dictData", dictData);
        result.put("announce", announce);
        result.put("job", job);
        result.put("jobLog", jobLog);
        result.put("file", file);
        result.put("gen", gen);
        result.put("total", total);
        return R.ok(result);
    }

    private Map<String, Object> fetch(RecycleCountEndpoint client) {
        try {
            R<Map<String, Object>> resp = client.count();
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.warn("[Recycle] 跨服务回收站计数失败 client={} err={}",
                    client.getClass().getSimpleName(), e.getMessage());
        }
        return Map.of();
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
