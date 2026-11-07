package com.wu.cloud.ai;

import com.wu.cloud.common.banner.StartupBanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 智能助手微服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableAsync
// 统一扫描 api 模块全部 Feign 契约，新增远程服务时无需再逐个登记
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@MapperScan("com.wu.cloud.ai.mapper")
public class AiApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AiApplication.class, args);
        StartupBanner.print(context);
    }
}
