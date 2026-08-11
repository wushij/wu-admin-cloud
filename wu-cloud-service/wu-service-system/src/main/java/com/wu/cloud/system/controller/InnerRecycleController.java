package com.wu.cloud.system.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.mapper.SysRecycleMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统服务回收站计数内部接口（P0 修复：infra 不再直查 wu_system 表）
 * <p>供 wu-service-infra 的 RecycleCenterController 通过 @LoadBalanced RestTemplate
 * 跨服务聚合 wu_system 库内各表的回收站计数。
 * 路径 /inner/** 受网关 InnerApiGuardFilter 保护，禁止外部访问。</p>
 */
@RestController
@RequestMapping("/inner/recycle")
public class InnerRecycleController {

    @Resource
    private SysRecycleMapper sysRecycleMapper;

    @GetMapping("/count")
    public R<Map<String, Long>> count() {
        Map<String, Long> result = new LinkedHashMap<>(8);
        result.put("user", sysRecycleMapper.countDeletedUsers(null, null, null, null, null));
        result.put("role", sysRecycleMapper.countDeletedRoles(null));
        result.put("menu", sysRecycleMapper.countDeletedMenus(null));
        result.put("dept", sysRecycleMapper.countDeletedDepts(null));
        result.put("post", sysRecycleMapper.countDeletedPosts());
        result.put("dictType", sysRecycleMapper.countDeletedDictTypes(null));
        result.put("dictData", sysRecycleMapper.countDeletedDictData(null, null));
        result.put("configGroup", sysRecycleMapper.countDeletedConfigGroups());
        return R.ok(result);
    }
}
