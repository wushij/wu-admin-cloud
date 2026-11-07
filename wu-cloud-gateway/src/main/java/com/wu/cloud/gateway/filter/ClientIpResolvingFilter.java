package com.wu.cloud.gateway.filter;

import com.wu.cloud.common.utils.web.TrustedProxyIpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 客户端真实 IP 归一化过滤器（Anti-Spoofing）。
 *
 * <p><b>问题</b>：客户端可以自由构造 {@code X-Real-IP} / {@code X-Forwarded-For}。
 * 若网关把这些头原样透传给下游，下游服务会把伪造的 IP 当成真实来源，
 * 从而绕过基于 IP 的封禁、登录锁定与限流。</p>
 *
 * <p><b>做法</b>：网关在入口处按「可信代理」规则算出唯一可信的客户端 IP，
 * 然后<b>覆盖重写</b>这三个头并向下游转发。顺序上：</p>
 * <ol>
 *   <li>先按可信代理规则取值：请求来自 Nginx（回环/内网）时采信 Nginx 设置的 X-Real-IP / XFF；</li>
 *   <li>直连网关（对端是公网地址）时一律使用 TCP 对端地址，忽略一切可伪造的头；</li>
 *   <li>重写后下游服务看到的头已由网关背书，其自身再解析时对端是网关（可信代理），口径一致。</li>
 * </ol>
 *
 * <p>顺序要求：必须早于 {@link SecurityRiskGlobalFilter} 与
 * {@link RequestRateLimitGlobalFilter}，否则它们读到的仍是客户端原始头。</p>
 */
@Component
public class ClientIpResolvingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolvingFilter.class);

    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_CF_CONNECTING_IP = "CF-Connecting-IP";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        InetSocketAddress address = request.getRemoteAddress();
        String remoteAddr = address == null || address.getAddress() == null
                ? null : address.getAddress().getHostAddress();

        String clientIp = TrustedProxyIpResolver.resolve(
                remoteAddr,
                request.getHeaders().getFirst(HEADER_REAL_IP),
                request.getHeaders().getFirst(HEADER_FORWARDED_FOR),
                request.getHeaders().getFirst(HEADER_CF_CONNECTING_IP));

        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    // 先删掉客户端自带的值，再写入网关判定后的可信 IP
                    h.remove(HEADER_REAL_IP);
                    h.remove(HEADER_FORWARDED_FOR);
                    h.remove(HEADER_CF_CONNECTING_IP);
                    h.set(HEADER_REAL_IP, clientIp);
                    h.set(HEADER_FORWARDED_FOR, clientIp);
                })
                .build();

        if (log.isDebugEnabled()) {
            log.debug("[ClientIp] 归一化客户端 IP: remote={} -> clientIp={}", remoteAddr, clientIp);
        }
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // 必须早于风控拦截与限流（Ordered.HIGHEST_PRECEDENCE + 8 / -100），
        // 否则它们读到的还是客户端可伪造的原始头
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
