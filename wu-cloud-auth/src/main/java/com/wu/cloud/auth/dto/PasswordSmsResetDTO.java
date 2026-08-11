package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通过短信验证码重置当前账号密码
 */
@Data
public class PasswordSmsResetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 短信验证码 */
    private String code;

    /** 新密码 */
    private String newPassword;

    /** 确认密码 */
    private String confirmPassword;
}
