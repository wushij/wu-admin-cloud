package com.wu.cloud.infra.framework;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 基础设施服务 WebMVC 配置：注册 API 访问日志采集拦截器
 */
@Configuration
public class InfraWebMvcConfiguration implements WebMvcConfigurer {

    @Resource
    private ApiAccessLogInterceptor apiAccessLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessLogInterceptor)
                .addPathPatterns("/**");
    }
}
