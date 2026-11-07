package com.wu.cloud.auth.support;

/**
 * 认证中心 Redis Key 统一定义
 */
public final class AuthRedisKeys {

    /** 图形验证码：auth:captcha:{uuid} */
    private static final String CAPTCHA = "auth:captcha:%s";

    /** 滑块验证码：auth:slider:{token} */
    private static final String SLIDER = "auth:slider:%s";

    /** 滑块通过凭证：auth:slider:pass:{token} */
    private static final String SLIDER_PASS = "auth:slider:pass:%s";

    /** 短信验证码：auth:sms:{scene}:{mobile} */
    private static final String SMS_CODE = "auth:sms:%s:%s";

    /** 邮箱验证码：auth:email:{scene}:{email} */
    private static final String EMAIL_CODE = "auth:email:%s:%s";

    /** 验证码发送间隔：auth:code:interval:{scene}:{target} */
    private static final String CODE_INTERVAL = "auth:code:interval:%s:%s";

    /** 验证码校验通过票据：auth:ticket:{scene}:{ticket} */
    private static final String CODE_TICKET = "auth:ticket:%s:%s";

    /** 登录失败次数：auth:login:fail:{username} */
    private static final String LOGIN_FAIL = "auth:login:fail:%s";

    /** 账号锁定：auth:login:lock:{username} */
    private static final String LOGIN_LOCK = "auth:login:lock:%s";

    /** 会话签名密钥：auth:session:sign:{sessionKey}（兼容旧版字段，已不再写入） */
    private static final String SESSION_SIGN = "auth:session:sign:%s";

    /** HMAC-SM3 签名密钥：security:session-sign:{clientId}（与前端/网关共用，与既有规范对齐前缀） */
    private static final String SECURITY_SESSION_SIGN = "security:session-sign:%s";

    /** SM4 加解密密钥：security:session-sm4:{clientId}（与前端/网关共用，与既有规范对齐前缀） */
    private static final String SECURITY_SESSION_SM4 = "security:session-sm4:%s";

    private AuthRedisKeys() {
    }

    public static String captcha(String uuid) {
        return String.format(CAPTCHA, uuid);
    }

    public static String slider(String token) {
        return String.format(SLIDER, token);
    }

    public static String sliderPass(String token) {
        return String.format(SLIDER_PASS, token);
    }

    public static String smsCode(String scene, String mobile) {
        return String.format(SMS_CODE, scene, mobile);
    }

    public static String emailCode(String scene, String email) {
        return String.format(EMAIL_CODE, scene, email);
    }

    public static String codeInterval(String scene, String target) {
        return String.format(CODE_INTERVAL, scene, target);
    }

    public static String codeTicket(String scene, String ticket) {
        return String.format(CODE_TICKET, scene, ticket);
    }

    public static String loginFail(String username) {
        return String.format(LOGIN_FAIL, username);
    }

    public static String loginLock(String username) {
        return String.format(LOGIN_LOCK, username);
    }

    public static String sessionSign(String sessionKey) {
        return String.format(SESSION_SIGN, sessionKey);
    }

    public static String securitySessionSign(String clientId) {
        return String.format(SECURITY_SESSION_SIGN, clientId);
    }

    public static String securitySessionSm4(String clientId) {
        return String.format(SECURITY_SESSION_SM4, clientId);
    }
}
