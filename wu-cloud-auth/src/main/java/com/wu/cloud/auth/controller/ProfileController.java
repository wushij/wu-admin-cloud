package com.wu.cloud.auth.controller;

import com.wu.cloud.api.log.Log;
import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.auth.dto.BindEmailDTO;
import com.wu.cloud.auth.dto.BindMobileDTO;
import com.wu.cloud.auth.dto.ChangePasswordDTO;
import com.wu.cloud.auth.dto.EmailCodeDTO;
import com.wu.cloud.auth.dto.PasswordEmailResetDTO;
import com.wu.cloud.auth.dto.PasswordSmsResetDTO;
import com.wu.cloud.auth.dto.ProfileUpdateDTO;
import com.wu.cloud.auth.dto.SmsCodeDTO;
import com.wu.cloud.auth.service.ProfileService;
import com.wu.cloud.auth.vo.ProfileVO;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 个人中心 REST API 控制层
 */
@RestController
@RequestMapping("/api/auth/profile")
public class ProfileController {

    @Resource
    private ProfileService profileService;

    // ==================== 个人资料 ====================

    /**
     * 获取个人信息
     */
    @GetMapping
    public R<ProfileVO> getProfile() {
        return R.ok(profileService.getProfile());
    }

    /**
     * 更新个人资料
     */
    @Log(title = "更新个人资料", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> updateProfile(@RequestBody ProfileUpdateDTO updateDTO) {
        profileService.updateProfile(updateDTO);
        return R.ok(true, "更新成功");
    }

    /**
     * 上传头像
     */
    @Log(title = "上传头像", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String url = profileService.uploadAvatar(file);
        return R.ok(Map.of("url", url), "头像上传成功");
    }

    // ==================== 密码 ====================

    /**
     * 使用原密码修改密码
     */
    @Log(title = "修改密码", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/password")
    public R<Boolean> changePassword(@RequestBody ChangePasswordDTO changeDTO) {
        profileService.changePassword(changeDTO);
        return R.ok(true, "密码修改成功，请重新登录");
    }

    /**
     * 发送短信重置密码验证码
     */
    @PostMapping("/password/sms-code")
    public R<Map<String, String>> sendPasswordSmsCode() {
        return R.ok(Map.of("mobile", profileService.sendPasswordSmsCode()), "验证码已发送");
    }

    /**
     * 通过短信验证码重置密码
     */
    @Log(title = "短信重置密码", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/password/sms-reset")
    public R<Boolean> resetPasswordBySms(@RequestBody PasswordSmsResetDTO resetDTO) {
        profileService.resetPasswordBySms(resetDTO);
        return R.ok(true, "密码重置成功，请重新登录");
    }

    /**
     * 发送邮箱重置密码验证码
     */
    @PostMapping("/password/email-code")
    public R<Map<String, String>> sendPasswordEmailCode() {
        return R.ok(Map.of("email", profileService.sendPasswordEmailCode()), "验证码已发送");
    }

    /**
     * 通过邮箱验证码重置密码
     */
    @Log(title = "邮箱重置密码", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @PutMapping("/password/email-reset")
    public R<Boolean> resetPasswordByEmail(@RequestBody PasswordEmailResetDTO resetDTO) {
        profileService.resetPasswordByEmail(resetDTO);
        return R.ok(true, "密码重置成功，请重新登录");
    }

    // ==================== 手机号绑定 ====================

    /**
     * 发送绑定手机号验证码
     */
    @PostMapping("/mobile/sms-code")
    public R<Boolean> sendMobileBindCode(@RequestBody SmsCodeDTO smsCodeDTO) {
        if (smsCodeDTO == null || !StringUtils.hasText(smsCodeDTO.getMobile())) {
            throw new BusinessException("手机号不能为空");
        }
        profileService.sendMobileBindCode(smsCodeDTO.getMobile());
        return R.ok(true, "验证码已发送");
    }

    /**
     * 绑定手机号
     */
    @Log(title = "绑定手机号", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/mobile")
    public R<Boolean> bindMobile(@RequestBody BindMobileDTO bindDTO) {
        profileService.bindMobile(bindDTO);
        return R.ok(true, "手机号绑定成功");
    }

    // ==================== 邮箱绑定 ====================

    /**
     * 发送绑定邮箱验证码
     */
    @PostMapping("/email/code")
    public R<Boolean> sendEmailBindCode(@RequestBody EmailCodeDTO emailCodeDTO) {
        if (emailCodeDTO == null || !StringUtils.hasText(emailCodeDTO.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }
        profileService.sendEmailBindCode(emailCodeDTO.getEmail());
        return R.ok(true, "验证码已发送");
    }

    /**
     * 绑定邮箱
     */
    @Log(title = "绑定邮箱", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/email")
    public R<Boolean> bindEmail(@RequestBody BindEmailDTO bindDTO) {
        profileService.bindEmail(bindDTO);
        return R.ok(true, "邮箱绑定成功");
    }

    // ==================== 登录记录 ====================

    /**
     * 分页查询我的登录记录
     */
    @GetMapping("/login-logs")
    public R<PageResult<LoginLogDTO>> getMyLoginLogs(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return R.ok(profileService.getMyLoginLogs(pageNo, pageSize));
    }
}
