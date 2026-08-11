package com.wu.cloud.auth.service;

/**
 * 短信 / 邮箱验证码服务
 */
public interface VerifyCodeService {

    /** 场景：短信登录 */
    String SCENE_SMS_LOGIN = "sms_login";

    /** 场景：邮箱登录 */
    String SCENE_EMAIL_LOGIN = "email_login";

    /** 场景：注册 */
    String SCENE_REGISTER = "register";

    /** 场景：忘记密码 */
    String SCENE_FORGOT_PASSWORD = "forgot_password";

    /** 场景：绑定手机号 */
    String SCENE_BIND_MOBILE = "bind_mobile";

    /** 场景：绑定邮箱 */
    String SCENE_BIND_EMAIL = "bind_email";

    /** 场景：短信重置密码 */
    String SCENE_RESET_PASSWORD_SMS = "reset_password_sms";

    /** 场景：邮箱重置密码 */
    String SCENE_RESET_PASSWORD_EMAIL = "reset_password_email";

    /**
     * 发送短信验证码
     *
     * @param scene  业务场景
     * @param mobile 手机号
     */
    void sendSmsCode(String scene, String mobile);

    /**
     * 发送邮箱验证码
     *
     * @param scene 业务场景
     * @param email 邮箱
     */
    void sendEmailCode(String scene, String email);

    /**
     * 校验短信验证码，成功后立即失效
     */
    void verifySmsCode(String scene, String mobile, String code);

    /**
     * 校验邮箱验证码，成功后立即失效
     */
    void verifyEmailCode(String scene, String email, String code);

    /**
     * 校验验证码并签发一次性票据，用于分步骤流程（如忘记密码）
     *
     * @return 票据
     */
    String verifyAndIssueTicket(String scene, String target, String code, boolean isSms);

    /**
     * 消费票据，校验失败抛出业务异常
     *
     * @return 票据绑定的目标（手机号/邮箱）
     */
    String consumeTicket(String scene, String ticket);
}
