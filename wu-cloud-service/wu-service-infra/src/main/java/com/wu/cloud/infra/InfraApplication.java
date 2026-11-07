package com.wu.cloud.infra;

import com.wu.cloud.common.banner.StartupBanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 基础设施服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
// 统一扫描 api 模块全部 Feign 契约（配置组加载、日志上报等）
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@MapperScan("com.wu.cloud.infra.mapper")
public class InfraApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(InfraApplication.class, args);
        StartupBanner.print(context);
    }
}
