package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送短信验证码请求
 */
@Data
public class SmsCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String mobile;

    /** 业务场景，默认短信登录 */
    private String scene;
}
