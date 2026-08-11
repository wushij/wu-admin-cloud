package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 绑定手机号请求
 */
@Data
public class BindMobileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新手机号 */
    private String mobile;

    /** 短信验证码 */
    private String code;
}
