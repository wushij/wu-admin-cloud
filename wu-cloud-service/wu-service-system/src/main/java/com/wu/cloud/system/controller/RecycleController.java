package com.wu.cloud.system.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.mapper.SysRecycleMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 回收站汇总统计 REST 控制器（B 队 P1 修复：跨服务回收站计数）
 *
 * <p>聚合 system 库内表的回收站计数 + 通过 @LoadBalanced RestTemplate 调用
 * ticket/infra/file/message 服务的 /inner/recycle/count 端点获取跨服务计数。
 * 某服务不可用或超时时返回 0，不影响整体响应。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/system/recycle")
public class RecycleController {

    @Resource
    private SysRecycleMapper sysRecycleMapper;
    @Resource
    private RestTemplate restTemplate;

    @RequiresPermissions("system:recycle:query")
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

        // ===== 跨服务回收站计数（RestTemplate 调用，失败返回 0）=====
        long ticket = fetchCount("wu-service-ticket", "ticket");
        long approval = fetchCount("wu-service-ticket", "approval");
        long notice = fetchCount("wu-service-ticket", "notice");
        long announce = fetchCount("wu-service-message", "announce");
        long job = fetchCount("wu-service-infra", "job");
        long jobLog = fetchCount("wu-service-infra", "jobLog");
        long file = fetchCount("wu-service-file", "file");
        long gen = fetchCount("wu-service-file", "gen");

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

    /**
     * 通过 @LoadBalanced RestTemplate 调用目标服务的 /inner/recycle/count 端点
     * 返回的 R.data 是 Map<String, Long>，从中提取指定 key 的值
     */
    @SuppressWarnings("unchecked")
    private long fetchCount(String serviceName, String key) {
        try {
            String url = "http://" + serviceName + "/inner/recycle/count";
            R<Map<String, Object>> resp = restTemplate.getForObject(url, R.class);
            if (resp != null && resp.getCode() == 200 && resp.getData() != null) {
                Object val = resp.getData().get(key);
                if (val instanceof Number number) {
                    return number.longValue();
                }
            }
        } catch (Exception e) {
            log.warn("[Recycle] 跨服务回收站计数失败 service={} key={} err={}", serviceName, key, e.getMessage());
        }
        return 0L;
    }
}
