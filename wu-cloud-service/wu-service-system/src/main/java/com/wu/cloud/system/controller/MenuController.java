package com.wu.cloud.system.controller;

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
@RestController
@RequestMapping("/api/system/menu")
public class MenuController {

    @Resource
    private MenuService menuService;
    @Resource
    private SysRecycleService sysRecycleService;

    @RequiresPermissions("system:menu:list")
    @GetMapping("/list")
    public R<List<SysMenuDO>> getMenuList(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Integer type) {
        return R.ok(menuService.getMenuList(name, status, type));
    }

    @RequiresPermissions("system:menu:list")
    @GetMapping("/simple-list")
    public R<List<SysMenuDO>> getSimpleList() {
        return R.ok(menuService.getSimpleList());
    }

    @RequiresPermissions("system:menu:query")
    @GetMapping("/get")
    public R<SysMenuDO> getMenu(@RequestParam("id") Long id) {
        return R.ok(menuService.getMenu(id));
    }

    @RequiresPermissions("system:menu:add")
    @Log(title = "菜单管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Long> createMenu(@RequestBody SysMenuDO menuDO) {
        return R.ok(menuService.createMenu(menuDO));
    }

    @RequiresPermissions("system:menu:edit")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update")
    public R<Boolean> updateMenu(@RequestBody SysMenuDO menuDO) {
        menuService.updateMenu(menuDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:remove")
    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> deleteMenu(@RequestParam("id") Long id) {
        menuService.deleteMenu(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:edit")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update-status")
    public R<Boolean> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        menuService.updateStatus(id, status);
        return R.ok(true);
    }

    @RequiresPermissions("system:menu:list")
    @GetMapping("/recycle/page")
    public R<PageResult<SysMenuDO>> recyclePage(PageQuery pageQuery,
                                                @RequestParam(value = "name", required = false) String name,
                                                @RequestParam(value = "keyword", required = false) String keyword) {
        String kw = (name != null && !name.isBlank()) ? name : keyword;
        Page<SysMenuDO> page = sysRecycleService.getMenuRecyclePage(pageQuery, kw);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @RequiresPermissions("system:menu:edit")
    @Log(title = "菜单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.restoreMenu(id));
    }

    @RequiresPermissions("system:menu:edit")
    @PostMapping("/restore")
    public R<Boolean> restorePost(@RequestParam("id") Long id) {
        return restore(id);
    }

    @RequiresPermissions("system:menu:remove")
    @Log(title = "菜单管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        return R.ok(sysRecycleService.purgeMenu(id));
    }
}
