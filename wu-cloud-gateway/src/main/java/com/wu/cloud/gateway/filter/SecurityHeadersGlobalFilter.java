package com.wu.cloud.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 响应安全头过滤器（与单体 SecurityHeadersFilter 对齐）。
 * <p>为所有经由网关的响应补齐浏览器安全头，降低 MIME 嗅探、点击劫持与
 * 敏感响应被缓存的风险。</p>
 */
@Component
public class SecurityHeadersGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-XSS-Protection", "0");
        headers.set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        ServerHttpRequest request = exchange.getRequest();
        // 仅 HTTPS 请求下发 HSTS，避免本地 HTTP 调试被浏览器强制升级
        if (request.getURI().getScheme() != null
                && "https".equalsIgnoreCase(request.getURI().getScheme())) {
            headers.set("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains");
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
