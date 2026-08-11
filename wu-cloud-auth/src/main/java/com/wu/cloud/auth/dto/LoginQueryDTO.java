package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求 DTO
 *
 * <p>支持账号密码登录与短信验证码登录两种方式，由 loginType 区分。</p>
 */
@Data
public class LoginQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录方式：password 账号密码 / sms 短信验证码 */
    private String loginType = "password";

    /** 账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 手机号，短信登录时使用 */
    private String mobile;

    /** 短信验证码，短信登录时使用 */
    private String smsCode;

    /** 图形验证码标识 */
    private String uuid;

    /** 图形验证码 */
    private String captcha;

    /** 验证码兼容字段（前端传 code 时自动适配） */
    private String code;

    /** 滑块验证通过票据 */
    private String sliderTicket;

    public String getCaptcha() {
        return org.springframework.util.StringUtils.hasText(captcha) ? captcha : code;
    }
}
