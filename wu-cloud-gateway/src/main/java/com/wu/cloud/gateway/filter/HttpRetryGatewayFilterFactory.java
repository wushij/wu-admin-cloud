package com.wu.cloud.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * GET 重试过滤器，但跳过 WebSocket 升级。
 * <p>
 * 默认 {@code Retry} 把握手（GET + Upgrade）也包进 reactor Retry；
 * 一旦 {@code httpclient.response-timeout} 触发，会重试握手并掐断后续帧，
 * 表现为「第一条实时消息能收到，第二条要刷新才出现」。
 */
@Component
public class HttpRetryGatewayFilterFactory extends RetryGatewayFilterFactory {

    @Override
    public GatewayFilter apply(RetryConfig config) {
        GatewayFilter delegate = super.apply(config);
        return (exchange, chain) -> {
            if (isWebSocketUpgrade(exchange)) {
                return chain.filter(exchange);
            }
            return delegate.filter(exchange, chain);
        };
    }

    static boolean isWebSocketUpgrade(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String upgrade = headers.getUpgrade();
        if (upgrade != null && "websocket".equalsIgnoreCase(upgrade)) {
            return true;
        }
        return headers.getConnection().stream().anyMatch(v -> "upgrade".equalsIgnoreCase(v));
    }
}
