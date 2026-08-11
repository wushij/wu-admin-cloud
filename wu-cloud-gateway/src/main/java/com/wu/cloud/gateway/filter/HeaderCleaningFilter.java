package com.wu.cloud.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import com.wu.cloud.common.core.constant.HeaderConstants;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * 客户端 Header 强制清洗与 TraceId 注入过滤器 (Anti-Spoofing Filter)
 * 优先级最硬：Order(-100)，防止客户端恶意伪造 X-User-Id / X-User-Name 头透传至内网
 */
@Component
public class HeaderCleaningFilter implements GlobalFilter, Ordered {

    private static final Set<String> SECURE_HEADERS = Set.of(
            HeaderConstants.USER_ID,
            HeaderConstants.USER_NAME,
            HeaderConstants.USER_ROLES,
            HeaderConstants.USER_PERMISSIONS,
            HeaderConstants.TRACE_ID
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> SECURE_HEADERS.forEach(h::remove))
                .header(HeaderConstants.TRACE_ID, traceId)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
