package com.wu.cloud.common.utils.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.symmetric.SM4;
import java.nio.charset.StandardCharsets;

/**
 * 国密 SM2 / SM3 / SM4 加解密通用工具类
 */
public class SmCryptoUtils {

    /**
     * SM4 CBC 模式解密
     */
    public static String decryptSm4Cbc(String hexCiphertext, byte[] key, byte[] iv) {
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, key, iv);
        byte[] decrypted = sm4.decrypt(HexUtil.decodeHex(hexCiphertext));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * SM4 CBC 模式加密
     */
    public static String encryptSm4Cbc(String plaintext, byte[] key, byte[] iv) {
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, key, iv);
        byte[] encrypted = sm4.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return HexUtil.encodeHexStr(encrypted);
    }

    /**
     * HMAC-SM3 签名计算
     */
    public static String signHmacSm3(String data, byte[] key) {
        HMac hmac = SmUtil.hmacSm3(key);
        return hmac.digestHex(data, StandardCharsets.UTF_8);
    }
}
