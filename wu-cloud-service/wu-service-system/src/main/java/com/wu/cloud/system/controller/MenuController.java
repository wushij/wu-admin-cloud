package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.service.MenuService;
import com.wu.cloud.system.service.SysRecycleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单与权限 REST
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
public class MenuController {

    @Resource
    private MenuService menuService;
    @Resource
    private SysRecycleService sysRecycleService;

    @RequiresPermissions("system:menu:list")
    @Operation(summary = "获取菜单树（管理页）")
    @GetMapping("/list")
    public R<List<SysMenuDO>> getMenuList(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Integer type) {
        return R.ok(menuService.getMenuList(name, status, type));
    }

    @RequiresPermissions("system:menu:list")
    @Operation(summary = "获取菜单全量列表（角色分配等，扁平）")
    @GetMapping("/simple-list")
    public R<List<SysMenuDO>> getSimpleList() {
        return R.ok(menuService.getSimpleList());
    }

    @RequiresPermissions("system:menu:query")
    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    public R<SysMenuDO> getMenu(@RequestParam("id") Long id) {
        return R.ok(menuService.getMenu(id));
    }

    @RequiresPermissions("system:menu:create")
    @Log(title = "菜单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> createMenu(@RequestBody SysMenuDO menuDO) {
        return R.ok(menuService.createMenu(menuDO));
    }

    @RequiresPermissions("system:menu:update")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> updateMenu(@RequestBody SysMenuDO menuDO) {
        menuService.updateMenu(menuDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:delete")
    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:update")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新菜单状态")
    @PutMapping("/update-status")
    public R<Boolean> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        menuService.updateStatus(id, status);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:list")
    @Operation(summary = "菜单回收站分页")
    @GetMapping("/recycle/page")
    public R<PageResult<SysMenuDO>> recyclePage(PageQuery pageQuery,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "keyword", required = false) String keyword) {
        String kw = (name != null && !name.isBlank()) ? name : keyword;
        Page<SysMenuDO> page = sysRecycleService.getMenuRecyclePage(pageQuery, kw);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:menu:update")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.restoreMenu(id));
    }

    @RequiresPermissions("system:menu:update")
    @PostMapping("/restore")
    public R<Boolean> restorePost(@RequestParam("id") Long id) {
        return restore(id);
    }

    @RequiresPermissions("system:menu:delete")
    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.purgeMenu(id));
    }
}
