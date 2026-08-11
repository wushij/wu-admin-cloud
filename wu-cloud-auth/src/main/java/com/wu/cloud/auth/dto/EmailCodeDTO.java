package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送邮箱验证码请求
 */
@Data
public class EmailCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 邮箱 */
    private String email;

    /** 业务场景 */
    private String scene;
}
