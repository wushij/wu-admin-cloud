package com.wu.cloud.monitor.config;

import com.wu.cloud.monitor.controller.MonitorController;
import com.wu.cloud.monitor.interceptor.AccessLogInterceptor;
import com.wu.cloud.monitor.service.CacheMonitorService;
import com.wu.cloud.monitor.service.ServerMonitorService;
import com.wu.cloud.monitor.service.impl.CacheMonitorServiceImpl;
import com.wu.cloud.monitor.service.impl.ServerMonitorServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 监控 Starter 自动装配：服务器监控、Redis 缓存监控与请求访问日志。
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(MonitorProperties.class)
public class MonitorAutoConfiguration {

    /**
     * 服务器硬件指标采集
     */
    @Bean
    @ConditionalOnMissingBean(ServerMonitorService.class)
    @ConditionalOnProperty(prefix = "wu.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ServerMonitorService serverMonitorService() {
        return new ServerMonitorServiceImpl();
    }

    /**
     * Redis 缓存指标采集，仅在服务引入并配置了 Redis 时生效
     */
    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean(CacheMonitorService.class)
    @ConditionalOnProperty(prefix = "wu.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheMonitorService cacheMonitorService(StringRedisTemplate stringRedisTemplate) {
        return new CacheMonitorServiceImpl(stringRedisTemplate);
    }

    /**
     * 监控 API 端点
     */
    @Bean
    @ConditionalOnMissingBean(MonitorController.class)
    @ConditionalOnProperty(prefix = "wu.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MonitorController monitorController(
            ServerMonitorService serverMonitorService,
            org.springframework.beans.factory.ObjectProvider<CacheMonitorService> cacheMonitorServiceProvider) {
        return new MonitorController(serverMonitorService, cacheMonitorServiceProvider);
    }

    /**
     * 访问日志拦截器注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "wu.monitor.access-log", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer accessLogWebMvcConfigurer(MonitorProperties properties) {
        MonitorProperties.AccessLog accessLog = properties.getAccessLog();
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new AccessLogInterceptor(accessLog))
                        .addPathPatterns("/**")
                        .excludePathPatterns(accessLog.getExcludePatterns())
                        // 保证 traceId 在业务拦截器之前写入 MDC
                        .order(Integer.MIN_VALUE + 100);
            }
        };
    }
}
