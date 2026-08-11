package com.wu.cloud.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关鉴权白名单配置
 * <p>
 * 在 application.yml 的 {@code gateway.auth.whitelist} 下维护，无需鉴权的路由（登录、WebSocket、公共查询等）。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class AuthWhitelist {

    /** 免鉴权路径（Ant 风格，如 /api/auth/**、/ws/**） */
    private List<String> whitelist = List.of(
            "/api/auth/**",
            "/ws/**",
            "/api/ws/**",
            "/api/doc.html",
            "/api/webjars/**",
            "/api/swagger-ui/**",
            "/api/v3/api-docs/**",
            "/file/**",
            "/files/**",
            "/api/files/**"
    );

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
