package com.wu.cloud.ai.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AI 模块 API Key 加解密组件（对齐单体：密钥取自 sys_config_group.security.sm4SecretKey）
 */
@Component
public class AiKeyCrypto {

    private static final Logger log = LoggerFactory.getLogger(AiKeyCrypto.class);

    private static final String PREFIX_SM4 = "sm4:";
    private static final String PREFIX_PLAIN = "plain:";

    @Value("${ai.sm4-secret-key:}")
    private String configuredSm4SecretKey;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public String encrypt(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return "";
        }
        String sm4Key = resolveSm4SecretKey();
        if (StrUtil.isBlank(sm4Key)) {
            log.warn("系统安全配置未设置 sm4SecretKey，AI API Key 将以明文形式存储，请尽快在系统配置中补齐密钥");
            return PREFIX_PLAIN + plainKey;
        }
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, toSm4KeyBytes(sm4Key), ivBytes);
        return PREFIX_SM4 + HexUtil.encodeHexStr(ivBytes) + sm4.encryptHex(plainKey);
    }

    public String decrypt(String storedKey) {
        if (StrUtil.isBlank(storedKey)) {
            return "";
        }
        if (storedKey.startsWith(PREFIX_PLAIN)) {
            return storedKey.substring(PREFIX_PLAIN.length());
        }
        if (!storedKey.startsWith(PREFIX_SM4)) {
            return storedKey;
        }
        String cipherText = storedKey.substring(PREFIX_SM4.length());
        String sm4Key = resolveSm4SecretKey();
        if (StrUtil.isBlank(sm4Key) || cipherText.length() <= 32) {
            log.error("AI API Key 解密失败：SM4 密钥未配置或密文格式非法");
            return "";
        }
        try {
            byte[] ivBytes = HexUtil.decodeHex(cipherText.substring(0, 32));
            SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, toSm4KeyBytes(sm4Key), ivBytes);
            return sm4.decryptStr(cipherText.substring(32));
        } catch (Exception e) {
            log.error("AI API Key SM4 解密失败: {}", e.getMessage());
            return "";
        }
    }

    private String resolveSm4SecretKey() {
        if (StrUtil.isNotBlank(configuredSm4SecretKey)) {
            return configuredSm4SecretKey.trim();
        }
        try {
            String json = jdbcTemplate.query(
                    "SELECT config_value FROM sys_config_group WHERE group_code = 'security' AND deleted = 0 LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : null);
            if (StrUtil.isBlank(json)) {
                return "";
            }
            JSONObject obj = JSONUtil.parseObj(json);
            return StrUtil.blankToDefault(obj.getStr("sm4SecretKey"), "").trim();
        } catch (Exception e) {
            log.warn("读取系统安全配置 sm4SecretKey 失败: {}", e.getMessage());
            return "";
        }
    }

    private static byte[] toSm4KeyBytes(String key) {
        if (key.length() == 32 && key.matches("[0-9a-fA-F]{32}")) {
            return HexUtil.decodeHex(key);
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
