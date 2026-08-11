package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通过邮箱验证码重置当前账号密码
 */
@Data
public class PasswordEmailResetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 邮箱验证码 */
    private String code;

    /** 新密码 */
    private String newPassword;

    /** 确认密码 */
    private String confirmPassword;
}
