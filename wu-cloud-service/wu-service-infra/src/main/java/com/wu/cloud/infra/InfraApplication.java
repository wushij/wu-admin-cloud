package com.wu.cloud.infra;

import com.wu.cloud.api.system.feign.RemoteUserService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 基础设施服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = RemoteUserService.class)
@MapperScan("com.wu.cloud.infra.mapper")
public class InfraApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraApplication.class, args);
    }
}
