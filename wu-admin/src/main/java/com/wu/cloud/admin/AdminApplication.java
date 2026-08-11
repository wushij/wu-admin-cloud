package com.wu.cloud.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 聚合服务入口 (Admin BFF / 兼容层) 启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wu.cloud.api")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
