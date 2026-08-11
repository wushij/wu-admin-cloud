package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 确认密码 */
    private String confirmPassword;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 短信验证码 */
    private String smsCode;

    /** 图形验证码标识 */
    private String uuid;

    /** 图形验证码 */
    private String captcha;
}
