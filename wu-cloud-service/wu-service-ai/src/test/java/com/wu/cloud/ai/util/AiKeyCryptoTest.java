package com.wu.cloud.ai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiKeyCrypto 单元测试")
class AiKeyCryptoTest {

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AiKeyCrypto crypto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(crypto, "configuredSm4SecretKey", SM4_KEY);
    }

    @Test
    @DisplayName("encrypt/decrypt：SM4 往返一致")
    void encryptDecrypt_roundTrip() {
        String cipher = crypto.encrypt("sk-test-secret");
        assertTrue(cipher.startsWith("sm4:"));
        assertEquals("sk-test-secret", crypto.decrypt(cipher));
    }

    @Test
    @DisplayName("decrypt：plain 前缀直接返回")
    void decrypt_plainPrefix() {
        assertEquals("raw-key", crypto.decrypt("plain:raw-key"));
    }

    @Test
    @DisplayName("encrypt：无密钥时降级为 plain 前缀")
    void encrypt_withoutKey_usesPlainPrefix() {
        ReflectionTestUtils.setField(crypto, "configuredSm4SecretKey", "");
        // configured 为空且 JdbcTemplate 未 stub（返回 null）时走明文前缀降级

        String stored = crypto.encrypt("sk-plain");
        assertEquals("plain:sk-plain", stored);
        assertEquals("sk-plain", crypto.decrypt(stored));
    }

    @Test
    @DisplayName("encrypt：空白入参返回空串")
    void encrypt_blank_returnsEmpty() {
        assertEquals("", crypto.encrypt("  "));
        assertEquals("", crypto.encrypt(null));
    }
}

