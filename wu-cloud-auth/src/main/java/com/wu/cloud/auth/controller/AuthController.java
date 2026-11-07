package com.wu.cloud.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.auth.dto.EmailCodeDTO;
import com.wu.cloud.auth.dto.ForgotPasswordCheckDTO;
import com.wu.cloud.auth.dto.ForgotPasswordResetDTO;
import com.wu.cloud.auth.dto.ForgotPasswordSmsCodeDTO;
import com.wu.cloud.auth.dto.LoginQueryDTO;
import com.wu.cloud.auth.dto.RegisterDTO;
import com.wu.cloud.auth.dto.SmsCodeDTO;
import com.wu.cloud.auth.dto.SmsCodeVerifyDTO;
import com.wu.cloud.auth.service.AuthService;
import com.wu.cloud.auth.service.CaptchaService;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.vo.CaptchaVO;
import com.wu.cloud.auth.vo.ForgotPasswordCheckVO;
import com.wu.cloud.auth.vo.LoginConfigVO;
import com.wu.cloud.auth.vo.LoginVO;
import com.wu.cloud.auth.vo.SessionSignInitVO;
import com.wu.cloud.auth.vo.SliderCaptchaVO;
import com.wu.cloud.auth.vo.UserInfoVO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一认证中心 REST API 控制层
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private VerifyCodeService verifyCodeService;

    // ==================== 登录与登出 ====================

    @Log(title = "用户登录", businessType = Log.BusinessType.LOGIN, isSaveResponseData = false)
    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginQueryDTO query) {
        return R.ok(authService.login(query), "登录成功");
    }

    @Log(title = "用户登出", businessType = Log.BusinessType.LOGOUT)
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Boolean> logout() {
        authService.logout();
        return R.ok(true, "退出成功");
    }

    /**
     * 当前登录用户信息（前端 getInfo → /auth/info）
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<UserInfoVO> info() {
        return R.ok(authService.getUserInfo());
    }

    // ==================== 登录辅助 ====================

    /**
     * 获取登录页配置
     */
    @Operation(summary = "获取登录配置")
    @GetMapping("/config")
    public R<LoginConfigVO> getConfig() {
        return R.ok(authService.getLoginConfig());
    }

    /**
     * 获取图形验证码
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> getCaptcha() {
        return R.ok(captchaService.generateImageCaptcha());
    }

    /**
     * 获取滑块验证码挑战
     */
    @Operation(summary = "获取滑块验证码 challenge")
    @GetMapping("/slider-challenge")
    public R<SliderCaptchaVO> getSliderChallenge() {
        return R.ok(captchaService.generateSliderChallenge());
    }

    /**
     * 校验滑块位移，返回登录可用票据
     */
    @Operation(summary = "校验滑块验证码并签发一次性票据")
    @PostMapping("/slider-verify")
    public R<Map<String, String>> verifySlider(@RequestParam("token") String token,
                                               @RequestParam("x") Integer x) {
        return R.ok(Map.of("ticket", captchaService.verifySlider(token, x)), "验证通过");
    }

    /**
     * 初始化会话签名，返回会话级动态密钥（与旧版前端期望结构一致）
     */
    @Operation(summary = "初始化会话签名密钥（安全加固）")
    @PostMapping("/session-sign-init")
    public R<SessionSignInitVO> sessionSignInit(@RequestParam("clientId") String clientId) {
        return R.ok(authService.initSessionSign(clientId));
    }

    // ==================== 验证码下发与校验 ====================

    /**
     * 发送短信验证码，默认用于短信登录
     */
    @Log(title = "发送短信验证码", businessType = Log.BusinessType.OTHER)
    @Operation(summary = "发送登录短信验证码")
    @PostMapping("/sms-code")
    public R<Boolean> sendSmsCode(@RequestBody SmsCodeDTO smsCodeDTO) {
        if (smsCodeDTO == null || !StringUtils.hasText(smsCodeDTO.getMobile())) {
            throw new BusinessException("手机号不能为空");
        }
        String scene = StringUtils.hasText(smsCodeDTO.getScene())
                ? smsCodeDTO.getScene() : VerifyCodeService.SCENE_SMS_LOGIN;
        verifyCodeService.sendSmsCode(scene, smsCodeDTO.getMobile());
        return R.ok(true, "验证码已发送");
    }

    /**
     * 校验短信验证码，返回一次性票据供后续步骤使用
     */
    @Operation(summary = "核验短信验证码")
    @PostMapping("/sms-code/verify")
    public R<Map<String, String>> verifySmsCode(@RequestBody SmsCodeVerifyDTO verifyDTO) {
        if (verifyDTO == null || !StringUtils.hasText(verifyDTO.getMobile())) {
            throw new BusinessException("手机号不能为空");
        }
        String scene = StringUtils.hasText(verifyDTO.getScene())
                ? verifyDTO.getScene() : VerifyCodeService.SCENE_SMS_LOGIN;
        String ticket = verifyCodeService.verifyAndIssueTicket(
                scene, verifyDTO.getMobile(), verifyDTO.getCode(), true);
        return R.ok(Map.of("ticket", ticket), "验证通过");
    }

    /**
     * 发送邮箱验证码
     */
    @Log(title = "发送邮箱验证码", businessType = Log.BusinessType.OTHER)
    @Operation(summary = "发送登录邮箱验证码")
    @PostMapping("/email-code")
    public R<Boolean> sendEmailCode(@RequestBody EmailCodeDTO emailCodeDTO) {
        if (emailCodeDTO == null || !StringUtils.hasText(emailCodeDTO.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }
        String scene = StringUtils.hasText(emailCodeDTO.getScene())
                ? emailCodeDTO.getScene() : VerifyCodeService.SCENE_EMAIL_LOGIN;
        verifyCodeService.sendEmailCode(scene, emailCodeDTO.getEmail());
        return R.ok(true, "验证码已发送");
    }

    // ==================== 注册 ====================

    @Log(title = "用户注册", businessType = Log.BusinessType.INSERT, isSaveResponseData = false)
    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<Long> register(@RequestBody RegisterDTO registerDTO) {
        return R.ok(authService.register(registerDTO), "注册成功");
    }

    // ==================== 忘记密码 ====================

    /**
     * 忘记密码-校验账号
     */
    @Operation(summary = "忘记密码-校验用户名")
    @PostMapping("/forgot-password/check")
    public R<ForgotPasswordCheckVO> forgotPasswordCheck(@RequestBody ForgotPasswordCheckDTO checkDTO) {
        return R.ok(authService.forgotPasswordCheck(checkDTO));
    }

    /**
     * 忘记密码-发送短信验证码
     */
    @Operation(summary = "忘记密码-发送短信验证码")
    @PostMapping("/forgot-password/sms-code")
    public R<Map<String, String>> forgotPasswordSmsCode(@RequestBody ForgotPasswordSmsCodeDTO smsCodeDTO) {
        String masked = authService.forgotPasswordSmsCode(smsCodeDTO);
        return R.ok(Map.of("mobile", masked), "验证码已发送");
    }

    /**
     * 忘记密码-重置密码
     */
    @Log(title = "忘记密码重置", businessType = Log.BusinessType.UPDATE, isSaveResponseData = false)
    @Operation(summary = "忘记密码-短信重置密码")
    @PostMapping("/forgot-password/reset")
    public R<Boolean> forgotPasswordReset(@RequestBody ForgotPasswordResetDTO resetDTO) {
        authService.forgotPasswordReset(resetDTO);
        return R.ok(true, "密码重置成功，请重新登录");
    }
}
