package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码-校验账号请求
 */
@Data
public class ForgotPasswordCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号 */
    private String username;
}
