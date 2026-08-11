package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 图形验证码响应
 */
@Data
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否开启验证码，false 时前端无需展示 */
    private Boolean enabled;

    /** 验证码唯一标识，登录时回传 */
    private String uuid;

    /** Base64 图片，格式 data:image/png;base64,xxx */
    private String img;
}
