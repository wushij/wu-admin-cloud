package com.wu.cloud.common.web;

import com.wu.cloud.common.core.context.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 公共 WebMvc 配置
 * <p>
 * 引入 wu-cloud-common 的 Servlet Web 微服务自动注册 {@link UserContextInterceptor}，
 * 使网关下发的 X-User-Id / X-User-Roles / X-User-Permissions 在下游还原为
 * {@link com.wu.cloud.common.core.context.UserContextHolder} 上下文，供
 * {@code @RequiresPermissions} 切面使用。
 * （WebFlux 网关模块不扫描此配置，无影响。）
 * </p>
 * <p>
 * 配置 {@code wu.security.inner-token} 后，/inner/** 内网端点启用零信任令牌校验。
 * </p>
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${wu.security.inner-token:}")
    private String innerToken;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor(innerToken))
                .addPathPatterns("/**")
                .order(Integer.MIN_VALUE);
    }
}
