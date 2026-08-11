package com.wu.cloud.ai;

import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 智能助手微服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableAsync
@EnableFeignClients(basePackageClasses = {RemoteOnlineUserService.class, RemoteUserService.class})
@MapperScan("com.wu.cloud.ai.mapper")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
