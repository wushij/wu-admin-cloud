package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.dto.SysUserPageDTO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.service.SysUserService;
import com.wu.cloud.system.vo.SysUserRoleReqVO;
import com.wu.cloud.system.vo.UserCreateReqVO;
import com.wu.cloud.system.vo.UserResetPwdReqVO;
import com.wu.cloud.system.vo.UserUpdateReqVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户 REST 控制器（路径与行为与既有规范对齐 + wu-admin-web）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @RequiresPermissions("system:user:list")
    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<List<SysUserDO>> list() {
        return R.ok(sysUserService.listAll());
    }

    @RequiresPermissions("system:user:list")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<SysUserDO>> pageUsers(SysUserPageDTO pageDTO) {
        return R.ok(sysUserService.pageUsers(pageDTO));
    }

    @RequiresPermissions("system:user:query")
    @Operation(summary = "获取菜单详情")
    @GetMapping("/get")
    public R<SysUserDO> getUser(@RequestParam("id") Long id) {
        return R.ok(sysUserService.getDetail(id));
    }

    @RequiresPermissions("system:user:create")
    @Log(title = "系统用户", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> createUser(@RequestBody UserCreateReqVO createReqVO) {
        return R.ok(sysUserService.createUser(createReqVO));
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> updateUser(@RequestBody UserUpdateReqVO updateReqVO) {
        return R.ok(sysUserService.updateUser(updateReqVO));
    }

    @RequiresPermissions("system:user:delete")
    @Log(title = "系统用户", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<Boolean> deleteUser(@RequestParam("id") Long id) {
        return R.ok(sysUserService.deleteUser(id));
    }

    @RequiresPermissions("system:user:query")
    @Operation(summary = "获取用户角色列表")
    @GetMapping("/get-role-ids")
    public R<List<Long>> getRoleIds(@RequestParam("userId") Long userId) {
        return R.ok(sysUserService.getUserRoleIds(userId));
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "分配用户角色")
    @PostMapping("/assign-role")
    public R<Boolean> assignRole(@RequestBody SysUserRoleReqVO reqVO) {
        sysUserService.assignUserRoles(reqVO);
        return R.ok(true);
    }

    /** 兼容旧路径 */
    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "分配")
    @PutMapping("/assign-roles")
    public R<Boolean> assignRoles(@RequestBody SysUserRoleReqVO reqVO) {
        sysUserService.assignRoles(reqVO);
        return R.ok(true);
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新菜单状态")
    @PutMapping("/update-status")
    public R<Boolean> updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        sysUserService.updateUserStatus(id, status);
        return R.ok(true);
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "重置用户密码")
    @PutMapping("/reset-password")
    public R<Boolean> resetPassword(@RequestParam(value = "id", required = false) Long id,
                                    @RequestParam(value = "password", required = false) String password,
                                    @RequestBody(required = false) UserResetPwdReqVO body) {
        if (id != null && password != null) {
            sysUserService.resetPassword(id, password);
        } else if (body != null) {
            sysUserService.resetUserPassword(body);
        } else {
            return R.fail("重置密码参数不完整");
        }
        return R.ok(true);
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "解除登录失败锁定")
    @PutMapping("/unlock-login")
    public R<Boolean> unlockLogin(@RequestParam("id") Long id) {
        sysUserService.unlockLogin(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "踢用户下线")
    @DeleteMapping("/kick-out")
    public R<Boolean> kickOut(@RequestParam("id") Long id) {
        sysUserService.kickOut(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "注册用户审核", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "审核")
    @PutMapping("/audit")
    public R<Boolean> auditUser(@RequestParam("id") Long id, @RequestParam("action") String action) {
        sysUserService.auditUser(id, action);
        return R.ok(true, "审核处理成功");
    }

    @RequiresPermissions("system:user:list")
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<SysUserDO>> recyclePage(PageQuery pageQuery,
                                                @RequestParam(value = "username", required = false) String username,
                                                @RequestParam(value = "mobile", required = false) String mobile,
                                                @RequestParam(value = "status", required = false) Integer status,
                                                @RequestParam(value = "deptId", required = false) Long deptId) {
        return R.ok(sysUserService.recyclePage(pageQuery, username, mobile, status, deptId));
    }

    @RequiresPermissions("system:user:update")
    @Log(title = "系统用户", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        sysUserService.restore(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:user:delete")
    @Log(title = "系统用户", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        sysUserService.deletePermanent(id);
        return R.ok(true);
    }
}
