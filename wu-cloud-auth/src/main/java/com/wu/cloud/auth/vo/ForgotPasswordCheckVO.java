package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码-账号校验响应
 */
@Data
public class ForgotPasswordCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号是否存在且可重置 */
    private Boolean exists;

    /** 脱敏后的手机号，用于前端提示 */
    private String maskedMobile;
}
