package com.wu.cloud.infra.config;

import com.wu.cloud.common.config.DynamicConfigHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 短信/邮件配置门面：从 DynamicConfigHelper 动态读取 sms/email 配置组，
 * 为后续短信/邮件发送逻辑迁移提供统一配置读取入口。
 */
@Component
public class MessagingConfigFacade {

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    // ===== SMS =====
    public boolean isSmsEnabled() { return dynamicConfigHelper.isSmsEnabled(); }
    public String getSmsProvider() { return dynamicConfigHelper.getSmsProvider(); }
    public String getSmsAccessKeyId() { return dynamicConfigHelper.getSmsAccessKeyId(); }
    public String getSmsAccessKeySecret() { return dynamicConfigHelper.getSmsAccessKeySecret(); }
    public String getSmsSignName() { return dynamicConfigHelper.getSmsSignName(); }

    // ===== Email =====
    public boolean isEmailEnabled() { return dynamicConfigHelper.isEmailEnabled(); }
    public String getEmailHost() { return dynamicConfigHelper.getEmailHost(); }
    public int getEmailPort() { return dynamicConfigHelper.getEmailPort(); }
    public String getEmailUsername() { return dynamicConfigHelper.getEmailUsername(); }
    public String getEmailPassword() { return dynamicConfigHelper.getEmailPassword(); }
    public String getEmailFromName() { return dynamicConfigHelper.getEmailFromName(); }
}
