package com.wu.cloud.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文件与基础设施微服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@MapperScan("com.wu.cloud.file.mapper")
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
