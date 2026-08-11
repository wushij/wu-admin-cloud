package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 绑定邮箱请求
 */
@Data
public class BindEmailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新邮箱 */
    private String email;

    /** 邮箱验证码 */
    private String code;
}
