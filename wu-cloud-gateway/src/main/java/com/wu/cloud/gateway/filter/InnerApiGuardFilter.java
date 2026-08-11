package com.wu.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内部接口防火墙（P1-5 安全加固）
 * <p>
 * 微服务间 OpenFeign 调用统一以 {@code /inner/**} 路径前缀暴露（见 wu-cloud-api 中各
 * RemoteXxxService 的 {@code @FeignClient(path = "/inner/...")}），且 Feign 通过服务名
 * 直连下游（lb://），<b>不经过本网关</b>。因此任何从网关入站、以 {@code /inner} 开头的请求
 * 都必然是外部直接调用，属于越权尝试（例如伪造 /inner/system/user/reset-password）。
 * </p>
 * <p>
 * 本过滤器在鉴权之前（最高优先级）直接拦截此类请求并返回 403，杜绝内部 RPC 端点被外部触达。
 * 服务间调用因不经网关，不受影响。
 * </p>
 */
@Component
public class InnerApiGuardFilter implements GlobalFilter, Ordered {

    /** 内部接口路径前缀（与 wu-cloud-api 中 Remote 接口的 path 约定一致） */
    private static final String INNER_PREFIX = "/inner";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (path.startsWith(INNER_PREFIX)) {
            return forbidden(exchange, path);
        }
        return chain.filter(exchange);
    }

    /**
     * 返回 403 且响应体与系统统一 R 结构对齐：{ "code": 403, "msg": "...", "data": null }
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 403);
        body.put("msg", "内部接口禁止外部访问");
        body.put("data", null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":403,\"msg\":\"内部接口禁止外部访问\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 必须在 SaReactorFilter 之前执行，避免内部接口被带 Token 的普通用户越权访问
        // 取 Integer.MIN_VALUE + 100，避免 HIGHEST_PRECEDENCE - 100 整数溢出变成正数导致顺序错乱
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
