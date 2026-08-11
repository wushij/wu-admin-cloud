package com.wu.cloud.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 认证微服务启动类（wu-cloud-auth :9200）
 *
 * <p>认证中心自身不持有业务库，用户数据通过 OpenFeign 调用 wu-service-system 获取。</p>
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@EnableScheduling
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
