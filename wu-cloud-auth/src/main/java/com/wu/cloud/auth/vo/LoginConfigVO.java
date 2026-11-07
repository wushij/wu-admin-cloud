package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 公开认证配置（与既有规范对齐 SystemConfigHelper#buildPublicConfig / 前端 AuthPublicConfig）
 */
@Data
public class LoginConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private SiteSection site;
    private LoginSection login;
    private RegisterSection register;
    private SecuritySection security;
    private AiSection ai;

    @Data
    public static class SiteSection implements Serializable {
        private static final long serialVersionUID = 1L;
        private String platformName;
        private String platformSubtitle;
        private String loginWelcome;
        private String registerTitle;
        private String copyright;
    }

    @Data
    public static class LoginSection implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean captchaEnabled;
        private String captchaType;
        private Boolean smsLoginEnabled;
        private Boolean smsLoginSliderCaptchaEnabled;
        private Boolean emailLoginEnabled;
        private Boolean emailLoginSliderCaptchaEnabled;
        private Boolean rememberMe;
        private Boolean smsEnabled;
        private Boolean emailEnabled;
        private Integer maxRetryCount;
        private Integer maxRetryCountIp;
        private Integer lockTime;
    }

    @Data
    public static class RegisterSection implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean enabled;
        private Boolean captchaEnabled;
        private String captchaType;
        private String defaultRoleCode;
        private Boolean needAudit;
        private Integer minPasswordLength;
    }

    @Data
    public static class SecuritySection implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean disableDevtool;
        private Boolean sm3SignEnabled;
        private Boolean sm2SignEnabled;
        private Boolean sm4EncryptEnabled;
        /** SM2 口令传输公钥：前端登录/注册/改密时加密密码使用，私钥永不下发 */
        private String sm2PublicKey;
    }

    @Data
    public static class AiSection implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean assistantEnabled;
    }
}

