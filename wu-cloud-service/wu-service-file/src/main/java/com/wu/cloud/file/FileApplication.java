package com.wu.cloud.file;

import com.wu.cloud.common.banner.StartupBanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 文件与基础设施微服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
// 文件服务需要调用 system 的配置接口（存储引擎配置）与 infra 的日志接口
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@MapperScan("com.wu.cloud.file.mapper")
public class FileApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FileApplication.class, args);
        StartupBanner.print(context);
    }
}
