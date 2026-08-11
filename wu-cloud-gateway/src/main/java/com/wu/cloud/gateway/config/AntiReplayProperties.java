package com.wu.cloud.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 防重放配置。
 *
 * <p>默认关闭：存量客户端（如 uni-app 端）可能未发送 X-Timestamp/X-Nonce，
 * 贸然开启会导致全站 403。建议先在测试环境开启并确认所有端都已携带这两个头，
 * 再在生产灰度放量。</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway.anti-replay")
public class AntiReplayProperties {

    /** 是否启用防重放校验 */
    private boolean enabled = false;

    /** 时间戳有效窗口（秒），同时作为 nonce 的 Redis 过期时间 */
    private long timestampWindowSeconds = 300L;

    /** 免校验路径前缀（回调、静态资源、WebSocket、文档等） */
    private List<String> excludePaths = new ArrayList<>(Arrays.asList(
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
}
