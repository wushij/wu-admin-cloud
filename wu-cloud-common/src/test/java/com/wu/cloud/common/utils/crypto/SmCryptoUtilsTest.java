package com.wu.cloud.common.utils.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SmCryptoUtils 单元测试")
class SmCryptoUtilsTest {

    private static final byte[] KEY = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV = "fedcba9876543210".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("SM4 CBC 加解密往返")
    void sm4RoundTrip() {
        String plain = "{\"user\":\"alice\",\"role\":\"admin\"}";
        String cipher = SmCryptoUtils.encryptSm4Cbc(plain, KEY, IV);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);
        assertEquals(plain, SmCryptoUtils.decryptSm4Cbc(cipher, KEY, IV));
    }

    @Test
    @DisplayName("HMAC-SM3 签名稳定且区分数据")
    void hmacSm3_stableAndDistinct() {
        String a = SmCryptoUtils.signHmacSm3("payload-a", KEY);
        String b = SmCryptoUtils.signHmacSm3("payload-a", KEY);
        String c = SmCryptoUtils.signHmacSm3("payload-b", KEY);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertFalse(a.isBlank());
    }
}
