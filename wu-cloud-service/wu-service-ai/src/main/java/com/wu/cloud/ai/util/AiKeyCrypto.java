package com.wu.cloud.ai.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.utils.crypto.Sm4GcmUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 模块 API Key 加解密组件
 * <p>
 * 新数据统一使用满血标准 SM4-GCM 认证加密（AEAD：12 字节随机 IV + 16 字节 GMAC Tag），
 * 密文带 {@code sm4-gcm:} 前缀；历史 {@code sm4:} 前缀的 SM4-CBC 数据仅保留读取兼容，不再写入。
 * 密钥取自系统配置 security 分组的 sm4SecretKey；未配置密钥时降级 {@code plain:} 前缀明文存储并告警。
 * </p>
 */
@Component
public class AiKeyCrypto {

    private static final Logger log = LoggerFactory.getLogger(AiKeyCrypto.class);

    private static final String PREFIX_SM4_GCM = "sm4-gcm:";
    private static final String PREFIX_SM4 = "sm4:";
    private static final String PREFIX_PLAIN = "plain:";

    @Value("${ai.sm4-secret-key:}")
    private String configuredSm4SecretKey;

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    /** 加密明文 API Key 用于入库（满血标准 SM4-GCM 认证加密），空串原样返回 */
    public String encrypt(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return "";
        }
        String sm4Key = resolveSm4SecretKey();
        if (StrUtil.isBlank(sm4Key)) {
            log.warn("系统安全配置未设置 sm4SecretKey，AI API Key 将以明文形式存储，请尽快在系统配置中补齐密钥");
            return PREFIX_PLAIN + plainKey;
        }
        try {
            // 统一使用 Sm4GcmUtil.resolveKeyBytes 解析密钥，避免与网关安全过滤器出现两套规则
            return PREFIX_SM4_GCM + Sm4GcmUtil.encryptToBase64(Sm4GcmUtil.resolveKeyBytes(sm4Key), plainKey);
        } catch (Exception e) {
            log.error("AI API Key SM4-GCM 加密失败: {}", e.getMessage());
            return PREFIX_PLAIN + plainKey;
        }
    }

    /** 解密库中密文（新数据走 SM4-GCM 认证解密，历史 sm4: 前缀数据仅保留只读兼容），返回明文 API Key；解密失败返回空串 */
    public String decrypt(String storedKey) {
        if (StrUtil.isBlank(storedKey)) {
            return "";
        }
        if (storedKey.startsWith(PREFIX_PLAIN)) {
            return storedKey.substring(PREFIX_PLAIN.length());
        }

        boolean isGcmCipher = storedKey.startsWith(PREFIX_SM4_GCM);
        boolean isCbcCipher = storedKey.startsWith(PREFIX_SM4);
        if (!isGcmCipher && !isCbcCipher) {
            // 历史无前缀明文：不依赖 sm4SecretKey，避免系统未配置密钥时把可用 Key 读成空串
            return storedKey;
        }

        String sm4Key = resolveSm4SecretKey();
        if (StrUtil.isBlank(sm4Key)) {
            log.error("AI API Key 解密失败：系统未配置 sm4SecretKey");
            return "";
        }
        byte[] keyBytes;
        try {
            keyBytes = Sm4GcmUtil.resolveKeyBytes(sm4Key);
        } catch (Exception e) {
            log.error("AI API Key 解密失败：{}", e.getMessage());
            return "";
        }

        // 1. SM4-GCM 新格式密文（认证失败直接返回空串，绝不降级为其他模式）
        if (isGcmCipher) {
            String cipherPayload = storedKey.substring(PREFIX_SM4_GCM.length());
            try {
                return Sm4GcmUtil.decryptFromBase64(keyBytes, cipherPayload);
            } catch (Exception e) {
                log.error("AI API Key SM4-GCM 认证解密失败: {}", e.getMessage());
                return "";
            }
        }

        // 2. 历史数据 SM4-CBC 格式 (sm4: 前缀) 只读兼容
        if (isCbcCipher) {
            String cipherText = storedKey.substring(PREFIX_SM4.length());
            if (cipherText.length() <= 32) {
                log.error("AI API Key 解密失败：CBC 密文格式非法");
                return "";
            }
            try {
                byte[] ivBytes = HexUtil.decodeHex(cipherText.substring(0, 32));
                SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
                return sm4.decryptStr(cipherText.substring(32));
            } catch (Exception e) {
                log.error("AI API Key SM4-CBC 解密失败: {}", e.getMessage());
                return "";
            }
        }

        // 历史数据无前缀时按明文处理
        return storedKey;
    }

    private String resolveSm4SecretKey() {
        if (StrUtil.isNotBlank(configuredSm4SecretKey)) {
            return configuredSm4SecretKey.trim();
        }
        try {
            return StrUtil.nullToEmpty(dynamicConfigHelper.getStr("security", "sm4SecretKey", "")).trim();
        } catch (Exception e) {
            log.warn("读取系统安全配置 sm4SecretKey 失败: {}", e.getMessage());
            return "";
        }
    }
}
