package com.wu.cloud.gateway.filter;

import com.wu.cloud.gateway.config.AntiReplayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 防重放攻击过滤器（时间戳 + Nonce 查重）。
 *
 * <p><b>背景</b>：旧版 {@code ApiSecurityFilter} 提供了「时间戳窗口校验 + Nonce 防重放 +
 * HMAC-SM3 验签 + SM4 收发加密」四件套，微服务改造时整体缺失。
 * 而前端 {@code request.ts} 仍在发送 {@code X-Timestamp / X-Nonce / X-Signature}，
 * 服务端却无人校验——安全能力实际处于「看起来有、其实没有」的状态。</p>
 *
 * <p>本过滤器先补齐其中<b>收益最高且无需密钥协商</b>的两项：时间戳窗口 + Nonce 查重，
 * 可有效阻断请求重放。验签/加密涉及前后端密钥协商，需配套改造 auth 的
 * {@code session-sign-init}（当前只返回 sessionKey/salt，与前端期望的签名密钥不一致），
 * 建议作为独立任务推进，详见评估报告。</p>
 *
 * <p><b>默认关闭</b>（{@code gateway.anti-replay.enabled=false}），避免存量客户端
 * （尤其是未发送这些头的 uni-app 端）被直接拦截；灰度验证后再开启。</p>
 */
@Component
public class AntiReplayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AntiReplayFilter.class);

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String NONCE_KEY_PREFIX = "gateway:nonce:";

    private final AntiReplayProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AntiReplayFilter(AntiReplayProperties properties, ReactiveStringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();

        // OPTIONS 预检、白名单路径直接放行
        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isExcluded(request.getURI().getPath())) {
            return chain.filter(exchange);
        }

        String timestampStr = request.getHeaders().getFirst(HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HEADER_NONCE);

        // 1) 时间戳窗口校验
        if (!StringUtils.hasText(timestampStr)) {
            return reject(exchange, "请求已被拒绝：缺失 X-Timestamp 请求头");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return reject(exchange, "请求已被拒绝：非法时间戳格式");
        }
        long diff = Math.abs(System.currentTimeMillis() - timestamp);
        if (diff > properties.getTimestampWindowSeconds() * 1000L) {
            return reject(exchange, "请求已被拒绝：时间戳无效或已过期");
        }

        // 2) Nonce 查重（setIfAbsent 原子写入，过期时间与时间戳窗口一致）
        if (!StringUtils.hasText(nonce)) {
            return reject(exchange, "请求已被拒绝：缺失 X-Nonce 请求头");
        }
        return redisTemplate.opsForValue()
                .setIfAbsent(NONCE_KEY_PREFIX + nonce, "1",
                        Duration.ofSeconds(properties.getTimestampWindowSeconds()))
                .defaultIfEmpty(Boolean.TRUE)
                .flatMap(success -> {
                    if (Boolean.FALSE.equals(success)) {
                        return reject(exchange, "请求已被拒绝：检测到重复请求 (Anti-Replay)");
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    // Redis 故障时不阻断业务（可用性优先），仅告警
                    log.warn("[AntiReplay] Nonce 校验异常，放行本次请求: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isExcluded(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : properties.getExcludePaths()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        // 响应已提交时 getHeaders() 为只读视图，写入会抛异常并中断连接，这里做保护
        if (!response.isCommitted()) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            } catch (UnsupportedOperationException ignored) {
                // 响应头已只读，保持原样
            }
        }
        String body = "{\"code\":403,\"msg\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在 Header 清洗之后、鉴权之前执行：无效重放请求应尽早拒绝，不必进入鉴权
        return -50;
    }
}
