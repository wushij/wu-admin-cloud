package com.wu.cloud.message.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息服务 MyBatis-Plus 插件配置
 * <p>必须注册 {@link PaginationInnerInterceptor}，否则私聊历史/群消息/公告等 selectPage
 * 不会在 SQL 层追加 LIMIT，导致全表加载、数据量大时 OOM。</p>
 */
@Configuration
public class MessageMyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
