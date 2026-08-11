package com.wu.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付模块敏感端点拦截（外部入站防火墙）
 * <p>
 * wu-service-pay 已完成真实接入（微信 V3 / 支付宝 RSA2 回调验签），外部真实支付流程
 * （下单 {@code /api/pay/create}、回调 {@code /api/pay/notify/wechat|alipay}、查询
 * {@code /api/pay/status|/order/{orderNo}}）必须放行。
 * </p>
 * <p>
 * 本过滤器仅屏蔽以下<b>非支付流程所需</b>的敏感/仿真端点，避免被滥用：
 * <ul>
 *   <li>{@code /api/pay/notify/simulate}：模拟支付成功回调，仅限本地联调，生产必须禁用；</li>
 *   <li>{@code /api/pay/reconcile}：支付对账（资金敏感，仅允许内网/内部调用）。</li>
 * </ul>
 * 内部服务间调用通过 {@code RemotePayService}（@FeignClient path="/inner/pay"）以服务名直连，
 * 不经过本网关，不受此过滤器影响。
 * </p>
 */
@Component
public class PayApiBlockFilter implements GlobalFilter, Ordered {

    /** 仿真回调：本地联调专用，生产屏蔽 */
    private static final String SIMULATE_NOTIFY = "/api/pay/notify/simulate";
    /** 对账：资金敏感，仅内网可调 */
    private static final String RECONCILE = "/api/pay/reconcile";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith(SIMULATE_NOTIFY) || path.startsWith(RECONCILE)) {
            return blocked(exchange);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> blocked(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 403);
        body.put("msg", "该支付端点未对外开放");
        body.put("data", null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":403,\"msg\":\"该支付端点未对外开放\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 与 InnerApiGuardFilter 同级，先于鉴权链，属于外部入站防火墙。
        // 注意：HIGHEST_PRECEDENCE = Integer.MIN_VALUE，减号会整数溢出变成正数导致优先级极低，
        // 故必须用 +100（与 InnerApiGuardFilter 保持一致）。
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
