package com.wu.cloud.gateway.filter;

import com.wu.cloud.common.security.risk.SecurityRedisKeys;
import com.wu.cloud.common.utils.web.TrustedProxyIpResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 风险封禁拦截过滤器（请求入口）。
 * <p>
 * 风控属主 wu-service-system 把封禁标记写入 Redis（{@code security:risk:block:{主体}}），
 * 本过滤器在网关最前端读取同一份标记：命中即返回 403，
 * 使「手动封禁 / 累计风险分自动封禁」在所有服务、所有接口上真正生效。
 * </p>
 * <p>
 * 键名取自 wu-cloud-common 的 {@link SecurityRedisKeys}，与 system 服务写入端共用同一份契约，
 * 不再各自复制常量，避免后续改名导致网关与属主读到不同的键。
 * Redis 异常时按放行处理，优先保证站点可用。
 * </p>
 */
@Component
public class SecurityRiskGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecurityRiskGlobalFilter.class);

    private final StringRedisTemplate redisTemplate;

    public SecurityRiskGlobalFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = resolveClientIp(exchange);
        if (ip != null && !ip.isBlank()) {
            try {
                if (isBlocked(ip)) {
                    log.warn("[RiskGate] 命中封禁，拒绝请求 ip={} uri={}", ip, exchange.getRequest().getPath());
                    return reject(exchange, ip);
                }
            } catch (Exception e) {
                // 风控存储异常按放行处理，避免 Redis 抖动导致全站不可用。
                // 打印异常类型而非仅 getMessage()：Redis 连接类异常的 message 常为 null，
                // 只打印 message 会得到「放行: null」这种无法定位问题的日志。
                log.warn("[RiskGate] 封禁状态读取异常，放行 ip={} uri={} error={}: {}",
                        ip, exchange.getRequest().getPath(),
                        e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        return chain.filter(exchange);
    }

    /** 主体键：ip:{ip} 为主，兼容升级前写入的裸 IP 键 */
    private boolean isBlocked(String ip) {
        Boolean blocked = redisTemplate.hasKey(SecurityRedisKeys.block("ip:" + ip));
        if (Boolean.TRUE.equals(blocked)) {
            return true;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(SecurityRedisKeys.block(ip)));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String ip) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":403,\"msg\":\"账号或 IP 已被临时封禁，如需申诉请联系管理员\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 取真实客户端 IP。
     *
     * <p>只有请求来自可信代理时才采信 X-Real-IP / X-Forwarded-For，
     * 否则使用 TCP 对端地址——否则攻击者伪造代理头即可绕过 IP 封禁。</p>
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        String remoteAddr = address == null || address.getAddress() == null
                ? null : address.getAddress().getHostAddress();
        return TrustedProxyIpResolver.resolve(
                remoteAddr,
                exchange.getRequest().getHeaders().getFirst("X-Real-IP"),
                exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"),
                exchange.getRequest().getHeaders().getFirst("CF-Connecting-IP"));
    }

    @Override
    public int getOrder() {
        // 早于鉴权过滤器执行，避免被封禁主体继续消耗后端资源
        return -100;
    }
}
