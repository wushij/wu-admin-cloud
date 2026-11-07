package com.wu.cloud.pay;

import com.wu.cloud.common.banner.StartupBanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 支付微服务启动类
 *
 * <p>P1-2 修复：补充 {@code @EnableFeignClients}。此前 pay 是唯一未开启 Feign 的服务，
 * 导致支付成功后无法反向通知订单服务，也无法写入本地消息表。</p>
 */
@SpringBootApplication(scanBasePackages = "com.wu.cloud")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wu.cloud.api")
@MapperScan("com.wu.cloud.pay.mapper")
public class PayApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PayApplication.class, args);
        StartupBanner.print(context);
    }
}
