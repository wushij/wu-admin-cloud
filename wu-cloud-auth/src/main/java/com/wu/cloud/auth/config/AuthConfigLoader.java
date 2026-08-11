package com.wu.cloud.auth.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.system.feign.RemoteConfigService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 system 服务读取 sys_config_group 配置，缓存在本地内存。
 * 读取逻辑对齐单体 SystemConfigHelper；关键开关在读取前主动刷新，避免改配置后最多等 60 秒。
 */
@Slf4j
@Component
public class AuthConfigLoader {

    private static final String CAPTCHA_TYPE_IMAGE = "image";
    private static final String CAPTCHA_TYPE_SLIDER = "slider";
    private static final String CAPTCHA_TYPE_SMS = "sms";

    @Resource
    private RemoteConfigService remoteConfigService;

    @Resource
    private AuthProperties authProperties;

    private final Map<String, JSONObject> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void refresh() {
        loadGroup("login");
        loadGroup("site");
        loadGroup("register");
        loadGroup("security");
        loadGroup("session");
        loadGroup("rateLimit");
        loadGroup("ai");
        loadGroup("sms");
        loadGroup("email");
    }

    /** 公开配置接口调用前刷新，保证后台改完立刻生效 */
    public void refreshPublicGroups() {
        loadGroup("login");
        loadGroup("site");
        loadGroup("register");
        loadGroup("security");
        loadGroup("ai");
        loadGroup("sms");
        loadGroup("email");
    }

    public void loadGroup(String groupCode) {
        try {
            R<Map<String, Object>> res = remoteConfigService.getGroup(groupCode);
            if (res == null || res.getData() == null) {
                log.warn("[AuthConfigLoader] 配置组 {} 返回空", groupCode);
                return;
            }
            Object configValue = res.getData().get("configValue");
            JSONObject parsed = parseConfigValue(configValue);
            if (parsed != null) {
                cache.put(groupCode, parsed);
            } else {
                log.warn("[AuthConfigLoader] 配置组 {} 的 configValue 无法解析: {}", groupCode, configValue);
            }
        } catch (Exception e) {
            log.warn("[AuthConfigLoader] 加载配置组 {} 失败: {}", groupCode, e.getMessage());
        }
    }

    private JSONObject parseConfigValue(Object configValue) {
        if (configValue == null) {
            return null;
        }
        if (configValue instanceof String str) {
            if (str.isBlank()) {
                return null;
            }
            return JSONUtil.parseObj(str);
        }
        if (configValue instanceof Map<?, ?> map) {
            return JSONUtil.parseObj(JSONUtil.toJsonStr(map));
        }
        if (configValue instanceof JSONObject json) {
            return json;
        }
        String text = String.valueOf(configValue);
        if (text.isBlank() || "null".equals(text)) {
            return null;
        }
        try {
            return JSONUtil.parseObj(text);
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject getGroup(String groupCode) {
        JSONObject cached = cache.get(groupCode);
        if (cached == null) {
            loadGroup(groupCode);
            return cache.getOrDefault(groupCode, new JSONObject());
        }
        return cached;
    }

    public boolean getBool(String groupCode, String key, boolean defaultValue) {
        return getGroup(groupCode).getBool(key, defaultValue);
    }

    public String getStr(String groupCode, String key, String defaultValue) {
        String val = getGroup(groupCode).getStr(key);
        return StrUtil.isNotBlank(val) ? val.trim() : defaultValue;
    }

    public int getInt(String groupCode, String key, int defaultValue) {
        return getGroup(groupCode).getInt(key, defaultValue);
    }

    // ---------- 基础信息 ----------
    public String getPlatformName() {
        return getStr("site", "platformName", "Admin Platform");
    }

    public String getPlatformSubtitle() {
        return getStr("site", "platformSubtitle", "统一运维 · 高效管控");
    }

    public String getLoginWelcome() {
        return getStr("site", "loginWelcome", "Welcome");
    }

    public String getRegisterTitle() {
        return getStr("site", "registerTitle", "Sign Up");
    }

    public String getCopyright() {
        return getStr("site", "copyright", "");
    }

    // ---------- 会话 ----------
    public int getSessionSignExpireHours() {
        int hours = getInt("session", "sessionSignExpireHours", 24);
        return Math.max(1, Math.min(hours, 120));
    }

    public long getSessionSignTtlMinutes() {
        return (long) getSessionSignExpireHours() * 60;
    }

    // ---------- 限流 ----------
    public int getSmsSendIntervalSeconds() {
        int sec = getInt("rateLimit", "smsSendIntervalSeconds", (int) authProperties.getCodeSendIntervalSeconds());
        if (sec < 30) {
            return 30;
        }
        return Math.min(sec, 300);
    }

    // ---------- 登录 / 注册 ----------
    public boolean isCaptchaEnabled() {
        // 登录开关改完要立刻生效，读取前刷新 login 组
        loadGroup("login");
        JSONObject login = getGroup("login");
        if (login.containsKey("captchaEnabled")) {
            return login.getBool("captchaEnabled");
        }
        return authProperties.isCaptchaEnabled();
    }

    public String getCaptchaType() {
        JSONObject login = getGroup("login");
        String type = login.getStr("captchaType", authProperties.getCaptchaType());
        if (StrUtil.isBlank(type) || CAPTCHA_TYPE_SMS.equals(type)) {
            return CAPTCHA_TYPE_IMAGE;
        }
        return CAPTCHA_TYPE_SLIDER.equals(type) ? CAPTCHA_TYPE_SLIDER : CAPTCHA_TYPE_IMAGE;
    }

    public boolean isSmsLoginEnabled() {
        JSONObject login = getGroup("login");
        if (login.containsKey("smsLoginEnabled")) {
            return login.getBool("smsLoginEnabled", false);
        }
        return CAPTCHA_TYPE_SMS.equals(login.getStr("captchaType", ""));
    }

    public boolean isSmsLoginSliderCaptchaEnabled() {
        if (!isSmsLoginEnabled()) {
            return false;
        }
        return getGroup("login").getBool("smsLoginSliderCaptchaEnabled", false);
    }

    public boolean isSmsEnabled() {
        return getBool("sms", "enabled", false);
    }

    public boolean isEmailEnabled() {
        return getBool("email", "enabled", false);
    }

    public boolean isEmailLoginEnabled() {
        if (!isEmailEnabled()) {
            return false;
        }
        return getGroup("login").getBool("emailLoginEnabled", false);
    }

    public boolean isEmailLoginSliderCaptchaEnabled() {
        if (!isEmailLoginEnabled()) {
            return false;
        }
        return getGroup("login").getBool("emailLoginSliderCaptchaEnabled", false);
    }

    public boolean isRememberMeEnabled() {
        return getGroup("login").getBool("rememberMe", true);
    }

    public int getMaxRetryCount() {
        int n = getInt("login", "maxRetryCount", authProperties.getLoginFailMaxCount());
        return n < 1 ? 5 : Math.min(n, 20);
    }

    public int getMaxRetryCountIp() {
        int n = getInt("login", "maxRetryCountIp", 20);
        return n < 1 ? 20 : Math.min(n, 50);
    }

    public int getLockTimeMinutes() {
        int n = getInt("login", "lockTime", (int) Math.max(1, authProperties.getLoginLockSeconds() / 60));
        return n < 1 ? 10 : Math.min(n, 120);
    }

    public long getLockTimeSeconds() {
        return (long) getLockTimeMinutes() * 60;
    }

    public boolean isRegisterEnabled() {
        return getBool("register", "enabled", authProperties.isRegisterEnabled());
    }

    public boolean isRegisterCaptchaEnabled() {
        JSONObject reg = getGroup("register");
        if (reg.containsKey("captchaEnabled")) {
            return reg.getBool("captchaEnabled", true);
        }
        return isCaptchaEnabled();
    }

    public String getRegisterCaptchaType() {
        JSONObject reg = getGroup("register");
        if (reg.containsKey("captchaType")) {
            String type = reg.getStr("captchaType", CAPTCHA_TYPE_IMAGE);
            return StrUtil.isBlank(type) ? CAPTCHA_TYPE_IMAGE : type;
        }
        return getCaptchaType();
    }

    public String getRegisterDefaultRoleCode() {
        String code = getStr("register", "defaultRoleCode", "user");
        return StrUtil.isBlank(code) ? "user" : code.trim();
    }

    public boolean isRegisterNeedAudit() {
        return getBool("register", "needAudit", false);
    }

    public int getRegisterMinPasswordLength() {
        int n = getInt("register", "minPasswordLength", authProperties.getPasswordMinLength());
        return n < 6 ? 6 : Math.min(n, 32);
    }

    // ---------- 安全 ----------
    public boolean isDisableDevtool() {
        return getBool("security", "disableDevtool", false);
    }

    public boolean isConcurrentLogin() {
        return getBool("security", "isConcurrent", false);
    }

    public boolean isSm4EncryptEnabled() {
        return getBool("security", "sm4EncryptEnabled", false);
    }

    public boolean isSm4EncryptEffective() {
        return isSm4EncryptEnabled();
    }

    public boolean isSm2SignEnabled() {
        return getBool("security", "sm2SignEnabled", false);
    }

    public boolean isSm2SignEffective() {
        return isSm2SignEnabled() || getBool("security", "sm3SignEnabled", false);
    }

    public boolean isAiAssistantEnabled() {
        return getBool("ai", "assistantEnabled", true);
    }
}
