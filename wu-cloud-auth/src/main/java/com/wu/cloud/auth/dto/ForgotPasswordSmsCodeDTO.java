package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码-发送短信验证码请求
 */
@Data
public class ForgotPasswordSmsCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号 */
    private String username;
}
