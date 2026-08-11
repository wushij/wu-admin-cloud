package com.wu.cloud.common.xss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 过滤配置
 */
@Data
@ConfigurationProperties(prefix = "wu.xss")
public class XssProperties {

    /** 是否启用 XSS 过滤 */
    private boolean enabled = true;

    /** 排除路径，支持 /path/** 前缀匹配 */
    private List<String> excludeUrls = new ArrayList<>(List.of(
            "/inner/**",
            "/actuator/**",
            "/v3/api-docs/**"
    ));
}
