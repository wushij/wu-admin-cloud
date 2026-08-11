package com.wu.cloud.system.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系统服务 MyBatis-Plus 插件配置类
 * <p>
 * 必须注册 {@link PaginationInnerInterceptor}，否则所有 {@code selectPage} 接口
 * 不会在 SQL 层追加 LIMIT，导致全表加载（用户分页/日志分页等性能灾难）。
 */
@Configuration
public class SystemMyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
