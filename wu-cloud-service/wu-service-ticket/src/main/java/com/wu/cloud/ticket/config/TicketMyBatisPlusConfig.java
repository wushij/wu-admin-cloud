package com.wu.cloud.ticket.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工单微服务 MyBatis-Plus 插件配置
 * <p>
 * 必须注册 {@link PaginationInnerInterceptor}，否则工单/审批分页接口不会在 SQL 层
 * 追加 LIMIT，导致全表加载。
 */
@Configuration
public class TicketMyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
