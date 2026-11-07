package com.wu.cloud.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.common.utils.web.TrustedProxyIpResolver;
import com.wu.cloud.gateway.config.GatewayConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 应用层滑动窗口限流（与单体 RequestRateLimitFilter 对齐，WebFlux 版本）。
 *
 * <p>按「客户端 IP + 路由分组」在 60 秒滑动窗口内计数，超限返回 429。
 * 计数、清理与写入在同一段 Lua 中原子完成，避免并发下的中间态导致放行量超限。
 * 限流阈值来自系统配置 rateLimit 分组，可在线调整；Redis 故障时按放行处理（fail-open），
 * 优先保证业务可用性。</p>
 */
@Component
public class RequestRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimitGlobalFilter.class);

    private static final long WINDOW_SECONDS = 60L;

    /** 原子滑动窗口：KEYS[1]=键，ARGV[1]=当前毫秒，ARGV[2]=窗口起点，ARGV[3]=唯一成员，ARGV[4]=TTL 秒，ARGV[5]=阈值上限。
     * 超限时不写入新成员，防止前端重试或自旋刷新导致雪崩自锁。
     */
    private static final RedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local cutoff = tonumber(ARGV[2])
            local member = ARGV[3]
            local ttl = tonumber(ARGV[4])
            local limit = tonumber(ARGV[5])
            redis.call('ZREMRANGEBYSCORE', key, 0, cutoff)
            local current = redis.call('ZCARD', key)
            if current >= limit then
                return current + 1
            end
            redis.call('ZADD', key, now, member)
            redis.call('EXPIRE', key, ttl)
            return current + 1
            """, Long.class);

    /** 静态资源、文档、WebSocket 握手、公开配置与文件直出路径不参与应用层敏感限流 */
    private static final Set<String> EXCLUDE_PREFIXES = Set.of(
            "/api/doc.html",
            "/api/v3/api-docs",
            "/api/swagger-ui",
            "/api/webjars",
            "/api/actuator",
            "/api/favicon.ico",
            "/api/error",
            "/api/ws",
            "/api/files/",
            "/api/auth/config",
            "/api/auth/session-sign-init"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayConfigLoader gatewayConfigLoader;

    public RequestRateLimitGlobalFilter(ReactiveStringRedisTemplate redisTemplate,
                                        GatewayConfigLoader gatewayConfigLoader) {
        this.redisTemplate = redisTemplate;
        this.gatewayConfigLoader = gatewayConfigLoader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isExcluded(path)) {
            return chain.filter(exchange);
        }

        String group = resolveGroup(path);
        int limit = resolveLimit(group);
        if (limit <= 0) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(request);
        String key = "security:rate:" + group + ":" + clientIp;
        long now = System.currentTimeMillis();
        return redisTemplate.execute(SLIDING_WINDOW_SCRIPT,
                        List.of(key),
                        String.valueOf(now),
                        String.valueOf(now - WINDOW_SECONDS * 1000L),
                        UUID.randomUUID().toString(),
                        String.valueOf(WINDOW_SECONDS + 60),
                        String.valueOf(limit))
                .next()
                .flatMap(count -> count != null && count > limit
                        ? reject(exchange)
                        : chain.filter(exchange))
                // Redis 异常时 fail-open，避免限流设施故障放大为业务不可用
                .onErrorResume(e -> {
                    log.warn("应用层限流读取失败，按放行处理: ip={} group={} reason={}",
                            clientIp, group, e.getMessage());
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isExcluded(String path) {
        if (StrUtil.isBlank(path)) {
            return true;
        }
        for (String prefix : EXCLUDE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveGroup(String path) {
        if (path.startsWith("/api/auth/")) {
            return "auth";
        }
        if (path.startsWith("/api/storage/files/")) {
            return "files";
        }
        return "api";
    }

    private int resolveLimit(String group) {
        return switch (group) {
            case "auth" -> gatewayConfigLoader.getInt("rateLimit", "authPerIpMinute", 120);
            case "files" -> gatewayConfigLoader.getInt("rateLimit", "filesPerIpMinute", 120);
            default -> gatewayConfigLoader.getInt("rateLimit", "apiPerIpMinute", 300);
        };
    }

    /**
     * 解析客户端 IP。
     *
     * <p>统一走 {@link TrustedProxyIpResolver}：只有请求来自可信代理时才采信
     * {@code X-Real-IP} / {@code X-Forwarded-For}，否则使用 TCP 对端地址。
     * 若直接采信 XFF 头部，攻击者伪造该头即可让每个请求都算在不同的 IP 上，
     * 限流与基于 IP 的封禁都会被绕过。</p>
     */
    private String resolveClientIp(ServerHttpRequest request) {
        InetSocketAddress address = request.getRemoteAddress();
        String remoteAddr = address == null || address.getAddress() == null
                ? null : address.getAddress().getHostAddress();
        return TrustedProxyIpResolver.resolve(
                remoteAddr,
                request.getHeaders().getFirst("X-Real-IP"),
                request.getHeaders().getFirst("X-Forwarded-For"),
                request.getHeaders().getFirst("CF-Connecting-IP"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 8;
    }
}
