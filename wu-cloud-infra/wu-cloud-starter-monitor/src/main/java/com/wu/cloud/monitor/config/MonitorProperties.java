package com.wu.cloud.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 监控与访问日志配置
 */
@Data
@ConfigurationProperties(prefix = "wu.monitor")
public class MonitorProperties {

    /** 是否暴露服务器/缓存监控端点 */
    private boolean enabled = true;

    /** 访问日志配置 */
    private AccessLog accessLog = new AccessLog();

    @Data
    public static class AccessLog {

        /** 是否开启请求访问日志 */
        private boolean enabled = true;

        /** 超过该毫秒数视为慢请求，以 WARN 级别输出 */
        private long slowThresholdMillis = 1000L;

        /** 是否打印请求参数 */
        private boolean logRequestParams = true;

        /** 请求参数最大打印长度，超出部分截断 */
        private int maxParamLength = 2000;

        /** 不记录访问日志的路径（Ant 风格） */
        private List<String> excludePatterns = new ArrayList<>(List.of(
                "/actuator/**",
                "/inner/**",
                "/error",
                "/favicon.ico",
                "/doc.html",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        ));

        /** 需要脱敏的参数名（不区分大小写） */
        private List<String> sensitiveParams = new ArrayList<>(List.of(
                "password", "oldPassword", "newPassword", "confirmPassword",
                "token", "secret", "apiKey", "privateKey", "idCard", "bankCard"
        ));
    }
}
