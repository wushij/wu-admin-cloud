package com.wu.cloud.infra.job;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 负载均衡 RestTemplate 配置（P1-4 修复）
 * <p>
 * 供 {@link OutboxCompensateJob} 通过服务名动态调用目标服务的内部 API，
 * 如 {@code http://wu-service-order/inner/order/notify-paid}。
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
