package com.wu.cloud.common.config;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 动态配置助手：对齐单体 SystemConfigHelper，从 sys_config_group 读取配置。
 * <p>
 * 有 JDBC 的服务通过 Mapper 查表；无 JDBC 的通过 Feign 获取。
 * 使用方在构造时传入 configValueLoader（groupCode -> configValue JSON 字符串）。
 * 启动后每 60 秒自动刷新缓存。
 * </p>
 */
@Slf4j
public class DynamicConfigHelper {

    public static final int PLATFORM_MAX_FILE_MB = 500;

    private static final String DEFAULT_ALLOWED_EXTENSIONS =
            "jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov";

    private final Function<String, String> configValueLoader;
    private final Map<String, JSONObject> cache = new ConcurrentHashMap<>();

    public DynamicConfigHelper(Function<String, String> configValueLoader) {
        this.configValueLoader = configValueLoader;
    }

    /**
     * 刷新所有已知配置组
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void refreshAll() {
        for (String group : cache.keySet()) {
            loadGroup(group);
        }
    }

    public void loadGroup(String groupCode) {
        try {
            String json = configValueLoader.apply(groupCode);
            if (json != null && !json.isBlank()) {
                cache.put(groupCode, JSONUtil.parseObj(json));
            }
        } catch (Exception e) {
            log.warn("[DynamicConfig] 加载配置组 {} 失败: {}", groupCode, e.getMessage());
        }
    }

    /**
     * 预加载指定配置组（服务启动时调用）
     */
    public void preload(String... groupCodes) {
        for (String code : groupCodes) {
            cache.putIfAbsent(code, new JSONObject());
            loadGroup(code);
        }
    }

    public JSONObject getGroup(String groupCode) {
        JSONObject obj = cache.get(groupCode);
        if (obj == null) {
            cache.putIfAbsent(groupCode, new JSONObject());
            loadGroup(groupCode);
            return cache.getOrDefault(groupCode, new JSONObject());
        }
        return obj;
    }

    public boolean getBool(String groupCode, String key, boolean defaultValue) {
        return getGroup(groupCode).getBool(key, defaultValue);
    }

    public String getStr(String groupCode, String key, String defaultValue) {
        String val = getGroup(groupCode).getStr(key);
        return val != null && !val.isBlank() ? val : defaultValue;
    }

    public int getInt(String groupCode, String key, int defaultValue) {
        return getGroup(groupCode).getInt(key, defaultValue);
    }

    public long getLong(String groupCode, String key, long defaultValue) {
        return getGroup(groupCode).getLong(key, defaultValue);
    }

    // ===== site =====
    public String getPlatformName() { return getStr("site", "platformName", "Admin Platform"); }
    public String getPlatformSubtitle() { return getStr("site", "platformSubtitle", "统一运维 · 高效管控"); }
    public String getLoginWelcome() { return getStr("site", "loginWelcome", "Welcome"); }
    public String getRegisterTitle() { return getStr("site", "registerTitle", "Sign Up"); }
    public String getCopyright() { return getStr("site", "copyright", ""); }

    // ===== session =====
    public int getTokenExpireHours() {
        int hours = getInt("session", "tokenExpireHours", 24);
        return Math.max(1, Math.min(hours, 720));
    }

    public int getSessionSignExpireHours() {
        int hours = getInt("session", "sessionSignExpireHours", 24);
        return Math.max(1, Math.min(hours, 120));
    }

    // ===== file =====
    public int getFileMaxSizeMb() {
        int n = getInt("file", "maxSizeMb", 50);
        return n < 1 ? 50 : Math.min(n, PLATFORM_MAX_FILE_MB);
    }

    public String getFileAllowedExtensions() {
        String ext = getStr("file", "allowedExtensions", DEFAULT_ALLOWED_EXTENSIONS);
        return ext.isBlank() ? DEFAULT_ALLOWED_EXTENSIONS : ext.trim();
    }

    // ===== rateLimit =====
    public int getCaptchaPerIpMinute() { return getInt("rateLimit", "captchaPerIpMinute", 10); }
    public int getLoginPerIpMinute() { return getInt("rateLimit", "loginPerIpMinute", 10); }
    public int getRegisterPerIpMinute() { return getInt("rateLimit", "registerPerIpMinute", 5); }
    public int getSmsPerIpMinute() { return getInt("rateLimit", "smsPerIpMinute", 5); }
    public int getAiChatPerUserMinute() { return getInt("rateLimit", "aiChatPerUserMinute", 20); }

    // ===== login =====
    public boolean isCaptchaEnabled() { return getBool("login", "captchaEnabled", false); }
    public String getCaptchaType() { return getStr("login", "captchaType", "image"); }
    public boolean isSmsLoginEnabled() { return getBool("login", "smsLoginEnabled", false); }
    public boolean isEmailLoginEnabled() { return getBool("login", "emailLoginEnabled", false); }
    public int getMaxRetryCount() {
        int n = getInt("login", "maxRetryCount", 5);
        return n < 1 ? 5 : Math.min(n, 20);
    }

    public int getLockTimeMinutes() {
        int n = getInt("login", "lockTime", 10);
        return n < 1 ? 10 : Math.min(n, 120);
    }

    // ===== register =====
    public boolean isRegisterEnabled() { return getBool("register", "enabled", true); }
    public boolean isRegisterNeedAudit() { return getBool("register", "needAudit", false); }
    public int getRegisterMinPasswordLength() { return getInt("register", "minPasswordLength", 6); }

    // ===== security =====
    public boolean isDisableDevtool() { return getBool("security", "disableDevtool", false); }
    public boolean isConcurrentLogin() { return getBool("security", "isConcurrent", false); }
    public boolean isSm4EncryptEnabled() { return getBool("security", "sm4EncryptEnabled", false); }
    public boolean isSm2SignEnabled() { return getBool("security", "sm2SignEnabled", false); }
    public boolean isTimestampEnabled() { return getBool("security", "timestampEnabled", false); }
    public boolean isNonceEnabled() { return getBool("security", "nonceEnabled", false); }

    // ===== sms =====
    public boolean isSmsEnabled() { return getBool("sms", "enabled", false); }
    public String getSmsProvider() { return getStr("sms", "provider", ""); }
    public String getSmsAccessKeyId() { return getStr("sms", "accessKeyId", ""); }
    public String getSmsAccessKeySecret() { return getStr("sms", "accessKeySecret", ""); }
    public String getSmsSignName() { return getStr("sms", "signName", ""); }

    // ===== email =====
    public boolean isEmailEnabled() { return getBool("email", "enabled", false); }
    public String getEmailHost() { return getStr("email", "host", ""); }
    public int getEmailPort() { return getInt("email", "port", 465); }
    public String getEmailUsername() { return getStr("email", "username", ""); }
    public String getEmailPassword() { return getStr("email", "password", ""); }
    public String getEmailFromName() { return getStr("email", "fromName", ""); }

    // ===== ai =====
    public boolean isAiAssistantEnabled() { return getBool("ai", "assistantEnabled", true); }
    public long getAiTokensPerUserDaily() { return getLong("ai", "tokensPerUserDaily", 0); }
    public String getAiGlobalKnowledge() { return getStr("ai", "globalKnowledge", ""); }
    public String getAiAnswerScope() {
        String scope = getStr("ai", "answerScope", "focus");
        return "open".equalsIgnoreCase(scope) ? "open" : "focus";
    }

    /** 工作台统计：每组配置只读一次缓存，避免重复查询 */
    public void fillDashboardConfigFields(Map<String, Object> stats) {
        stats.put("platformName", getPlatformName());
        stats.put("platformSubtitle", getPlatformSubtitle());
        stats.put("fileMaxSizeMb", getFileMaxSizeMb());
        stats.put("fileAllowedExtensions", getFileAllowedExtensions());
        stats.put("tokenExpireHours", getTokenExpireHours());
        stats.put("loginCaptchaEnabled", isCaptchaEnabled());
        stats.put("loginCaptchaType", getCaptchaType());
        stats.put("loginRememberMe", getBool("login", "rememberMe", true));
        stats.put("loginMaxRetryCount", getMaxRetryCount());
        stats.put("loginLockTimeMinutes", getLockTimeMinutes());
        stats.put("registerEnabled", isRegisterEnabled());
        stats.put("registerNeedAudit", isRegisterNeedAudit());
    }
}
