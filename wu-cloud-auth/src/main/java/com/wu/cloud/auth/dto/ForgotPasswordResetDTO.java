package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码-重置密码请求
 */
@Data
public class ForgotPasswordResetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号 */
    private String username;

    /** 短信验证码 */
    private String code;

    /** 新密码 */
    private String newPassword;

    /** 确认密码 */
    private String confirmPassword;
}
