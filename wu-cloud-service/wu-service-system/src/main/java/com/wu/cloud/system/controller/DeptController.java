package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.service.DeptService;
import com.wu.cloud.system.service.SysRecycleService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理 REST
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/dept")
public class DeptController {

    @Resource
    private DeptService deptService;
    @Resource
    private SysRecycleService sysRecycleService;

    @RequiresPermissions("system:dept:list")
    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<List<SysDeptDO>> getDeptList() {
        return R.ok(deptService.getDeptList());
    }

    @RequiresPermissions("system:dept:list")
    @Operation(summary = "获取树形结构")
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getDeptTree(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        List<SysDeptDO> all = deptService.getDeptList();
        if (StringUtils.hasText(name)) {
            String kw = name.trim();
            all = all.stream()
                    .filter(d -> d.getName() != null && d.getName().contains(kw))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            all = all.stream()
                    .filter(d -> status.equals(d.getStatus()))
                    .collect(Collectors.toList());
        }
        return R.ok(buildTree(all));
    }

    @RequiresPermissions("system:dept:query")
    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    public R<SysDeptDO> getDept(@RequestParam("id") Long id) {
        return R.ok(deptService.getDept(id));
    }

    @RequiresPermissions("system:dept:create")
    @Log(title = "部门管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> createDept(@RequestBody SysDeptDO deptDO) {
        return R.ok(deptService.createDept(deptDO));
    }

    @RequiresPermissions("system:dept:update")
    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> updateDept(@RequestBody SysDeptDO deptDO) {
        deptService.updateDept(deptDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:dept:delete")
    @Log(title = "部门管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:dept:update")
    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "移动")
    @PutMapping("/move")
    public R<Boolean> move(@RequestParam("id") Long id,
                           @RequestParam("parentId") Long parentId,
                           @RequestParam(value = "sort", required = false) Integer sort) {
        deptService.moveDept(id, parentId);
        if (sort != null) {
            SysDeptDO dept = deptService.getDept(id);
            if (dept != null) {
                dept.setSort(sort);
                deptService.updateDept(dept);
            }
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:dept:update")
    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新菜单状态")
    @PutMapping("/update-status")
    public R<Boolean> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        deptService.updateStatus(id, status);
        return R.ok(true);
    }

    @RequiresPermissions("system:dept:list")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<SysDeptDO>> recyclePage(PageQuery pageQuery,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "keyword", required = false) String keyword) {
        String kw = StringUtils.hasText(name) ? name : keyword;
        Page<SysDeptDO> page = sysRecycleService.getDeptRecyclePage(pageQuery, kw);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:dept:update")
    @Log(title = "部门管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.restoreDept(id));
    }

    /** 兼容旧 POST */
    @RequiresPermissions("system:dept:update")
    @Operation(summary = "恢复数据")
    @PostMapping("/restore")
    public R<Boolean> restorePost(@RequestParam("id") Long id) {
        return restore(id);
    }

    @RequiresPermissions("system:dept:delete")
    @Log(title = "部门管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.purgeDept(id));
    }

    private List<Map<String, Object>> buildTree(List<SysDeptDO> depts) {
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
        for (SysDeptDO d : depts) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", d.getId());
            node.put("name", d.getName());
            node.put("parentId", d.getParentId());
            node.put("sort", d.getSort());
            node.put("status", d.getStatus());
            node.put("leaderUserId", d.getLeaderUserId());
            node.put("phone", d.getPhone());
            node.put("email", d.getEmail());
            node.put("createTime", d.getCreateTime());
            node.put("updateTime", d.getUpdateTime());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysDeptDO d : depts) {
            Map<String, Object> node = nodeMap.get(d.getId());
            Long parentId = d.getParentId() == null ? 0L : d.getParentId();
            if (parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void sortTree(List<Map<String, Object>> nodes) {
        nodes.sort(Comparator.comparing(n -> {
            Object sort = n.get("sort");
            return sort instanceof Integer ? (Integer) sort : 0;
        }));
        for (Map<String, Object> node : nodes) {
            Object children = node.get("children");
            if (children instanceof List<?> list && !list.isEmpty()) {
                sortTree((List<Map<String, Object>>) children);
            }
        }
    }
}
