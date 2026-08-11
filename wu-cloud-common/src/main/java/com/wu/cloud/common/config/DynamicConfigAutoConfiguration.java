package com.wu.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 有 JDBC 的服务自动注册 DynamicConfigHelper，通过 JdbcTemplate 直接查 sys_config_group 表。
 * 使用 @ConditionalOnClass name 字符串形式，避免无 JDBC 的服务（auth/gateway）类加载失败。
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcTemplate")
public class DynamicConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DynamicConfigHelper.class)
    public DynamicConfigHelper dynamicConfigHelper(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        log.info("[DynamicConfig] 使用 JdbcTemplate 直接查表模式");
        DynamicConfigHelper helper = new DynamicConfigHelper(groupCode -> {
            try {
                return jdbcTemplate.queryForObject(
                        "SELECT config_value FROM sys_config_group WHERE group_code = ? AND deleted = 0 LIMIT 1",
                        String.class, groupCode);
            } catch (Exception e) {
                return null;
            }
        });
        helper.preload("site", "session", "file", "rateLimit", "login", "register", "security", "sms", "email", "ai");
        return helper;
    }
}
