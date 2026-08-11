package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 校验短信验证码请求
 */
@Data
public class SmsCodeVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String mobile;

    /** 验证码 */
    private String code;

    /** 业务场景 */
    private String scene;
}
