package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.service.RoleService;
import com.wu.cloud.system.service.SysRecycleService;
import com.wu.cloud.system.vo.AssignMenuReqVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色管理 REST 控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @Resource
    private SysRecycleService sysRecycleService;

    @RequiresPermissions("system:role:list")
    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public R<List<SysRoleDO>> getRoleList() {
        return R.ok(roleService.getRoleList());
    }

    @RequiresPermissions("system:role:list")
    @Operation(summary = "获取角色分页")
    @GetMapping("/page")
    public R<PageResult<SysRoleDO>> getRolePage(PageQuery pageQuery,
                                          @RequestParam(value = "name", required = false) String name,
                                          @RequestParam(value = "status", required = false) Integer status) {
        Page<SysRoleDO> page = roleService.pageRoles(pageQuery, name, status);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:role:query")
    @Operation(summary = "获取角色详情")
    @GetMapping("/get")
    public R<SysRoleDO> getRole(@RequestParam("id") Long id) {
        return R.ok(roleService.getRole(id));
    }

    @RequiresPermissions("system:role:create")
    @Log(title = "角色管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增角色")
    @PostMapping("/create")
    public R<Long> createRole(@RequestBody SysRoleDO roleDO) {
        return R.ok(roleService.createRole(roleDO));
    }

    @RequiresPermissions("system:role:update")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改角色")
    @PutMapping("/update")
    public R<Boolean> updateRole(@RequestBody SysRoleDO roleDO) {
        roleService.updateRole(roleDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:role:delete")
    @Log(title = "角色管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除角色")
    @DeleteMapping("/delete")
    public R<Boolean> deleteRole(@RequestParam("id") Long id) {
        roleService.deleteRole(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:role:query")
    @Operation(summary = "获取角色菜单列表")
    @GetMapping("/get-menu-ids")
    public R<Set<Long>> getMenuIds(@RequestParam("roleId") Long roleId) {
        return R.ok(roleService.getMenuIdsForAssign(roleId));
    }

    @RequiresPermissions("system:role:update")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "分配角色菜单")
    @PostMapping("/assign-menu")
    public R<Boolean> assignMenu(@RequestBody AssignMenuReqVO reqVO) {
        roleService.assignMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return R.ok(true);
    }

    @RequiresPermissions("system:role:update")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新角色状态")
    @PutMapping("/update-status")
    public R<Boolean> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        roleService.updateStatus(id, status);
        return R.ok(true);
    }

    // ==================== 回收站 ====================

    @RequiresPermissions("system:role:list")
    @Operation(summary = "角色回收站分页")
    @GetMapping("/recycle/page")
    public R<PageResult<SysRoleDO>> recyclePage(PageQuery pageQuery,
                                          @RequestParam(value = "keyword", required = false) String keyword) {
        Page<SysRoleDO> page = sysRecycleService.getRoleRecyclePage(pageQuery, keyword);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:role:update")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复角色")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.restoreRole(id));
    }

    @RequiresPermissions("system:role:update")
    @Log(title = "角色管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/restore")
    public R<Boolean> restoreByPost(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.restoreRole(id));
    }

    @RequiresPermissions("system:role:delete")
    @Log(title = "角色管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除角色")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.purgeRole(id));
    }
}
