package com.wu.cloud.gateway.handler;

import com.wu.cloud.common.core.constant.HeaderConstants;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关级全局异常处理
 * <p>
 * 将下游不可用（503）、路由未命中（404）等网关层异常统一返回 {@code R} 结构，
 * 避免前端收到 Spring 默认错误 JSON。
 * </p>
 */
@Order(-1)
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        int code;
        String msg;
        if (ex instanceof NotFoundException) {
            code = 404;
            msg = "服务或接口不存在";
            response.setStatusCode(HttpStatus.NOT_FOUND);
        } else {
            code = 503;
            msg = "服务暂时不可用，请稍后重试";
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        }

        String body = "{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}";
        if (response.getHeaders().containsKey(HeaderConstants.TRACE_ID)) {
            response.getHeaders().remove(HeaderConstants.TRACE_ID);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
