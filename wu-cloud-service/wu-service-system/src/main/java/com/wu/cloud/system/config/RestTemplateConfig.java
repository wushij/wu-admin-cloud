package com.wu.cloud.system.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 负载均衡 RestTemplate 配置（B 队 P1 修复：跨服务回收站计数）
 * <p>供 RecycleController 通过服务名调用各服务的 /inner/recycle/count 端点</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
