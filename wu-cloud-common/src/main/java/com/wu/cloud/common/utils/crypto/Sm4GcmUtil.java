package com.wu.cloud.common.utils.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * 国密 SM4-GCM 满血标准认证加解密工具类（AEAD 模式）
 *
 * <p>标准依据：GM/T 0002-2012 (SM4 分组密码算法) / GM/T 0010-2012 (分组密码工作模式)。
 * 具备机密性保障与防止重放、篡改、密文填充攻击能力（带 128 位 GMAC 认证标签），
 * 与旧版后端、前端及移动端（TypeScript / BouncyCastle 互通规范）逐字节对齐。</p>
 */
public final class Sm4GcmUtil {

    /** 认证标签长度：128 位 (16 字节) */
    public static final int TAG_BIT_LENGTH = 128;
    /** GCM 模式推荐向量长度：96 位 (12 字节) */
    public static final int DEFAULT_IV_LENGTH = 12;
    /** SM4 密钥长度固定为 16 字节 (128 位) */
    public static final int SM4_KEY_LENGTH = 16;
    /** 最小有效载荷长度：12 字节 IV + 16 字节 Tag = 28 字节 */
    public static final int MIN_PAYLOAD_BYTES = DEFAULT_IV_LENGTH + (TAG_BIT_LENGTH / 8);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4GcmUtil() {
    }

    /**
     * 生成安全的随机 IV 向量 (12 字节)
     */
    public static byte[] generateIv() {
        byte[] iv = new byte[DEFAULT_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * 解析密钥字符串为 16 字节二进制材料。
     * 支持 32 位 Hex 字符串（会话签名协商下发），及 16 字符 ASCII/Latin-1 字符串。
     */
    public static byte[] resolveKeyBytes(String keyStr) {
        if (StrUtil.isBlank(keyStr)) {
            throw new IllegalArgumentException("SM4 密钥不能为空");
        }
        String trimmed = keyStr.trim();
        if (trimmed.length() == 32 && trimmed.matches("^[0-9a-fA-F]{32}$")) {
            return HexUtil.decodeHex(trimmed);
        }
        byte[] bytes = trimmed.getBytes(StandardCharsets.ISO_8859_1);
        if (bytes.length != SM4_KEY_LENGTH) {
            bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM4 密钥必须为 16 字节 (128位)，当前解码后长度为 " + bytes.length + " 字节");
        }
        return bytes;
    }

    /**
     * SM4-GCM 认证加密
     *
     * @param key        16 字节密钥
     * @param iv         12 字节 IV
     * @param plainBytes 明文字节数组
     * @param aad        附加认证数据 (可为 null)
     * @return 包含认证 Tag 的密文字节数组 (Ciphertext + 16B Tag)
     */
    public static byte[] encrypt(byte[] key, byte[] iv, byte[] plainBytes, byte[] aad) throws Exception {
        GCMModeCipher cipher = GCMBlockCipher.newInstance(new SM4Engine());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), TAG_BIT_LENGTH, iv, aad);
        cipher.init(true, parameters);

        byte[] out = new byte[cipher.getOutputSize(plainBytes.length)];
        int len = cipher.processBytes(plainBytes, 0, plainBytes.length, out, 0);
        cipher.doFinal(out, len);
        return out;
    }

    /**
     * SM4-GCM 认证解密
     *
     * @param key           16 字节密钥
     * @param iv            12 字节 IV
     * @param cipherWithTag 密文与认证 Tag (至少 16 字节)
     * @param aad           附加认证数据 (须与加密一致，可为 null)
     * @return 解密还原的明文字节数组
     */
    public static byte[] decrypt(byte[] key, byte[] iv, byte[] cipherWithTag, byte[] aad) throws Exception {
        GCMModeCipher cipher = GCMBlockCipher.newInstance(new SM4Engine());
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), TAG_BIT_LENGTH, iv, aad);
        cipher.init(false, parameters);

        byte[] out = new byte[cipher.getOutputSize(cipherWithTag.length)];
        int len = cipher.processBytes(cipherWithTag, 0, cipherWithTag.length, out, 0);
        cipher.doFinal(out, len);
        return out;
    }

    /**
     * 加密为标准 Base64 字符串：Base64(IV[12] + Ciphertext + Tag[16])
     */
    public static String encryptToBase64(byte[] keyBytes, String plainText) throws Exception {
        if (plainText == null) {
            return null;
        }
        byte[] iv = generateIv();
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] cipherWithTag = encrypt(keyBytes, iv, plainBytes, null);

        byte[] combined = new byte[iv.length + cipherWithTag.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherWithTag, 0, combined, iv.length, cipherWithTag.length);

        return Base64.toBase64String(combined);
    }

    /**
     * 加密为标准 Base64 字符串（重载方法，传入密钥字符串）
     */
    public static String encryptToBase64(String keyStr, String plainText) throws Exception {
        return encryptToBase64(resolveKeyBytes(keyStr), plainText);
    }

    /**
     * 从标准 Base64 密文解密：解析 Base64(IV[12] + Ciphertext + Tag[16])
     */
    public static String decryptFromBase64(byte[] keyBytes, String base64Payload) throws Exception {
        if (StrUtil.isBlank(base64Payload)) {
            return base64Payload;
        }
        byte[] combined = Base64.decode(base64Payload.trim());
        if (combined.length < MIN_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("SM4-GCM 密文长度非法，有效载荷至少需 " + MIN_PAYLOAD_BYTES + " 字节");
        }

        byte[] iv = new byte[DEFAULT_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, DEFAULT_IV_LENGTH);

        int cipherWithTagLen = combined.length - DEFAULT_IV_LENGTH;
        byte[] cipherWithTag = new byte[cipherWithTagLen];
        System.arraycopy(combined, DEFAULT_IV_LENGTH, cipherWithTag, 0, cipherWithTagLen);

        byte[] plainBytes = decrypt(keyBytes, iv, cipherWithTag, null);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 从标准 Base64 密文解密（重载方法，传入密钥字符串）
     */
    public static String decryptFromBase64(String keyStr, String base64Payload) throws Exception {
        return decryptFromBase64(resolveKeyBytes(keyStr), base64Payload);
    }

    /**
     * 快速判断字符串是否可能是 SM4-GCM Base64 密文格式
     */
    public static boolean isSm4GcmBase64Payload(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        String trimmed = str.trim();
        // 28 字节经 Base64 编码后至少为 38 字符
        if (trimmed.length() < 38) {
            return false;
        }
        return trimmed.matches("^[A-Za-z0-9+/=]+$");
    }
}
