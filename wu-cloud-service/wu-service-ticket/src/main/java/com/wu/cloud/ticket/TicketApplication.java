package com.wu.cloud.ticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 工单与审批流微服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@MapperScan("com.wu.cloud.ticket.mapper")
public class TicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
