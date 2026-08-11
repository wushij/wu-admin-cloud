package com.wu.cloud.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 接口安全过滤器配置（签名校验 + SM4 收发加密）。
 *
 * <p>与单体 {@code ApiSecurityFilter} 行为对齐：当 {@link #enabled} 为 true 时（对应单体
 * 「系统安全开关 sm2SignEnabled / sm4EncryptEnabled 已生效」），对所有非公开、非排除接口
 * 强制 HMAC-SM3 验签 + SM4 收发加密；缺失 {@code X-Client-Id}/{@code X-Timestamp}/
 * {@code X-Nonce}/{@code X-Signature} 即 403，与单体严格模式完全一致。</p>
 *
 * <p>会话密钥由认证中心 {@code /api/auth/session-sign-init} 按 clientId 下发并写入 Redis
 * （{@code security:session-sign:{clientId}} / {@code security:session-sm4:{clientId}}），
 * 本过滤器按请求头 {@code X-Client-Id} 取用。</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway.api-security")
public class ApiSecurityProperties {

    /** 是否启用接口签名/加密校验（= 单体全局安全开关） */
    private boolean enabled = true;

    /** 时间戳有效窗口（秒），同时作为 nonce 的 Redis 过期时间，对齐单体 5 分钟窗口 */
    private long timestampWindowSeconds = 300L;

    /** 免校验路径前缀（登录、验证码、回调、静态资源、WebSocket、文档、文件下载等） */
    private List<String> excludePaths = new ArrayList<>(Arrays.asList(
            "/api/auth/session-sign-init",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/config",
            "/api/auth/captcha",
            "/api/auth/sms-code",
            "/api/auth/slider-challenge",
            "/api/auth/slider-verify",
            "/api/auth/forgot-password",
            "/api/auth/refresh-token",
            "/api/pay/notify",
            "/api/file/",
            "/api/files/",
            "/file/",
            "/files/",
            "/api/ws",
            "/ws",
            "/api/doc.html",
            "/api/v3/api-docs",
            "/api/swagger-ui",
            "/api/webjars",
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/favicon.ico"
    ));

    /** 公开免验签接口（与单体 PUBLIC_AUTH_URIS 对齐），即使不带 clientId 也放行 */
    private List<String> publicAuthUris = new ArrayList<>(Arrays.asList(
            "/api/auth/session-sign-init",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/config",
            "/api/auth/captcha",
            "/api/auth/sms-code",
            "/api/auth/slider-challenge"
    ));

    /** 流式响应接口后缀（与单体 STREAMING_URI_SUFFIXES 对齐）：保留验签但响应不整体加密 */
    private List<String> streamingUriSuffixes = new ArrayList<>(List.of("/ai/chat/stream"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getTimestampWindowSeconds() {
        return timestampWindowSeconds;
    }

    public void setTimestampWindowSeconds(long timestampWindowSeconds) {
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getPublicAuthUris() {
        return publicAuthUris;
    }

    public void setPublicAuthUris(List<String> publicAuthUris) {
        this.publicAuthUris = publicAuthUris;
    }

    public List<String> getStreamingUriSuffixes() {
        return streamingUriSuffixes;
    }

    public void setStreamingUriSuffixes(List<String> streamingUriSuffixes) {
        this.streamingUriSuffixes = streamingUriSuffixes;
    }
}
