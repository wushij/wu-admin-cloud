package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    /** 确认密码 */
    private String confirmPassword;
}
