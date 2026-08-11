package com.wu.cloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网关 Sentinel 限流熔断配置（P0-3）。
 * <p>基于 sentinel-spring-cloud-gateway-adapter 为网关按路由维度配置流控规则，
 * 对被限流请求返回 429。配合网关级全局过滤器实现"外部流量入口限流"。</p>
 */
@Configuration
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                 ObjectProvider<ServerCodecConfigurer> serverCodecConfigurerProvider) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurerProvider.getIfAvailable(ServerCodecConfigurer::create);
    }

    /**
     * 限流命中时的统一响应处理器（返回 429 + JSON）
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 限流过滤器（注册为全局过滤器，作用于所有路由）
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 初始化 API 分组与网关流控规则
     */
    @PostConstruct
    public void initRules() {
        initApiDefinitions();
        initGatewayRules();
        initBlockHandler();
    }

    private void initApiDefinitions() {
        // 内部接口分组：全局限流更严，防止外部通过 inner 前缀调用
        Set<ApiDefinition> definitions = new HashSet<>();
        Set<ApiPredicateItem> innerPredicates = new HashSet<>();
        innerPredicates.add(new ApiPathPredicateItem().setPattern("/api/inner/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        definitions.add(new ApiDefinition("inner_api").setPredicateItems(innerPredicates));

        Set<ApiPredicateItem> authPredicates = new HashSet<>();
        authPredicates.add(new ApiPathPredicateItem().setPattern("/api/auth/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
        definitions.add(new ApiDefinition("auth_api").setPredicateItems(authPredicates));

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        // 登录接口：每秒最多 20，避免暴力破解
        rules.add(new GatewayFlowRule("auth_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(20).setIntervalSec(1));
        // 内部接口：每秒最多 100，防止异常突发
        rules.add(new GatewayFlowRule("inner_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(100).setIntervalSec(1));
        // 支付接口：QPS 限制 50，防止支付风暴
        rules.add(new GatewayFlowRule("/api/pay/**")
                .setCount(50).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }

    private void initBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, t) -> {
            Map<String, Object> body = new HashMap<>();
            body.put("code", 429);
            body.put("msg", "请求过于频繁，已被网关限流，请稍后再试");
            body.put("data", null);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        };
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}
