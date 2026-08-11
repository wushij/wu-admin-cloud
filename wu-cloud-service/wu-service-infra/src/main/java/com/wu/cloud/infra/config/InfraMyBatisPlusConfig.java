package com.wu.cloud.infra.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基础设施微服务 MyBatis-Plus 插件配置。
 * <p>必须注册 {@link PaginationInnerInterceptor}，否则 {@code selectPage}
 * 不会在 SQL 层追加 LIMIT，访问日志等分页接口会一次返回全表数据。</p>
 */
@Configuration
public class InfraMyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
