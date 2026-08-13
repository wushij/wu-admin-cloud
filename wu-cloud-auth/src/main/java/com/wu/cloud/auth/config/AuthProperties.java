package com.wu.cloud.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证中心业务参数配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wu.auth")
public class AuthProperties {

    /** 是否开启图形验证码 */
    private boolean captchaEnabled = true;

    /** 验证码类型：image 图形验证码 / slider 滑块验证码 */
    private String captchaType = "image";

    /** 验证码有效期（秒） */
    private long captchaExpireSeconds = 300;

    /** 是否允许注册 */
    private boolean registerEnabled = true;

    /** 是否允许短信登录 */
    private boolean smsLoginEnabled = true;

    /** 短信/邮箱验证码有效期（秒） */
    private long codeExpireSeconds = 300;

    /** 同一号码发送验证码的最小间隔（秒） */
    private long codeSendIntervalSeconds = 60;

    /** 登录失败达到该次数后锁定账号 */
    private int loginFailMaxCount = 5;

    /** 账号锁定时长（秒） */
    private long loginLockSeconds = 600;

    /** 滑块拼图允许的横向误差（像素） */
    private int sliderTolerance = 5;

    /** 密码最小长度 */
    private int passwordMinLength = 6;

    /** 是否启用接口签名/加密（会话级动态密钥，前端按 session-sign-init 返回的 enabled 生效） */
    private boolean sm3SignEnabled = false;

    /** 是否启用响应体 SM4 加密 */
    private boolean sm4EncryptEnabled = false;

    /** 会话签名密钥有效期（秒），与单体默认 1800 一致 */
    private long sessionSignExpireSeconds = 1800;

    /**
     * 公开 /auth/config：缓存齐全且未过 TTL 时跳过全量 Feign 刷新。
     * 设为 false 可回退为每次强制 refreshPublicGroups。
     */
    private boolean skipPublicRefreshWhenCached = true;

    /** 公开配置内存缓存 TTL（秒），到期后下次 /auth/config 会并行刷新 */
    private int publicConfigCacheTtlSeconds = 30;
}
