package com.wu.cloud.system.controller;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.service.SysUserService;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户个人中心（兼容入口）。
 * <p>前端正式路径为 {@code /api/auth/profile}（wu-cloud-auth）。
 * 本控制器仅作内部/兼容保留，新功能请勿再扩展。</p>
 */
@RestController
@RequestMapping("/api/system/user/profile")
public class ProfileController {

    @Resource
    private SysUserService sysUserService;

    @GetMapping("/get")
    public R<SysUserDO> getUserProfile() {
        Long userId = UserContextHolder.getUserId();
        // 修复：此前未登录时兜底返回 userId=1（超管），构成越权/信息泄露，
        // 现改为要求登录态，未登录一律 401。
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return R.ok(sysUserService.getUserById(userId));
    }

    @PutMapping("/update")
    public R<Boolean> updateProfile(@RequestBody SysUserDO userDO) {
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            userDO.setId(userId);
        }
        sysUserService.updateUser(userDO);
        return R.ok(true, "修改个人资料成功");
    }

    /**
     * 修改密码：校验旧密码 → 加密新密码 → 持久化
     */
    @PutMapping("/update-password")
    public R<Boolean> updatePassword(@RequestBody Map<String, String> params) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "原密码与新密码不能为空");
        }

        SysUserDO user = sysUserService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }

        String encodedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        sysUserService.resetPasswordDirectly(userId, encodedNewPassword);
        return R.ok(true, "修改密码成功");
    }
}
