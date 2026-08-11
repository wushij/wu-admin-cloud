package com.wu.cloud.infra.redis.config;

import com.wu.cloud.infra.redis.service.RedisService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot 官方标准 Starter 自动配置类
 */
@AutoConfiguration
public class RedisAutoConfiguration {

    @Bean
    public RedisService redisService() {
        return new RedisService();
    }
}
