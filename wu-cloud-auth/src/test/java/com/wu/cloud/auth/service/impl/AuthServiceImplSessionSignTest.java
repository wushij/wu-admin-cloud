package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.auth.vo.SessionSignInitVO;
import com.wu.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl.initSessionSign 单元测试")
class AuthServiceImplSessionSignTest {

    @Mock
    private AuthConfigLoader authConfigLoader;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("clientId 过短：拒绝")
    void initSessionSign_invalidClientId_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.initSessionSign("abc"));
        assertEquals("客户端标识不合法", ex.getMessage());
    }

    @Test
    @DisplayName("安全未生效：enabled=false 且不下发密钥")
    void initSessionSign_disabled_returnsEnabledFalse() {
        when(authConfigLoader.isSm2SignEffective()).thenReturn(false);
        when(authConfigLoader.isSm4EncryptEffective()).thenReturn(false);

        SessionSignInitVO vo = authService.initSessionSign("client-id-01");
        assertFalse(vo.getEnabled());
        assertNull(vo.getSm3SignKey());
        assertNull(vo.getSm4Key());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("SM4 生效：写入 Redis 并返回 32 位 hex 密钥")
    void initSessionSign_sm4Active_writesRedis() {
        when(authConfigLoader.isSm2SignEffective()).thenReturn(false);
        when(authConfigLoader.isSm4EncryptEffective()).thenReturn(true);
        when(authConfigLoader.getSessionSignTtlMinutes()).thenReturn(30L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        SessionSignInitVO vo = authService.initSessionSign("client-id-01");

        assertTrue(vo.getEnabled());
        assertNotNull(vo.getSm4Key());
        assertEquals(32, vo.getSm4Key().length());
        assertTrue(vo.getSm4Key().matches("[0-9a-f]{32}"));
        assertNull(vo.getSm3SignKey());
        verify(valueOps).set(eq(AuthRedisKeys.securitySessionSm4("client-id-01")),
                eq(vo.getSm4Key()), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("签名+加密均生效：同时下发 sm3/sm4 密钥")
    void initSessionSign_bothActive_writesBothKeys() {
        when(authConfigLoader.isSm2SignEffective()).thenReturn(true);
        when(authConfigLoader.isSm4EncryptEffective()).thenReturn(true);
        when(authConfigLoader.getSessionSignTtlMinutes()).thenReturn(60L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        SessionSignInitVO vo = authService.initSessionSign("client-id-99");

        assertTrue(vo.getEnabled());
        assertEquals(64, vo.getSm3SignKey().length());
        assertEquals(32, vo.getSm4Key().length());
        verify(valueOps).set(eq(AuthRedisKeys.securitySessionSign("client-id-99")),
                eq(vo.getSm3SignKey()), eq(Duration.ofMinutes(60)));
        verify(valueOps).set(eq(AuthRedisKeys.securitySessionSm4("client-id-99")),
                eq(vo.getSm4Key()), eq(Duration.ofMinutes(60)));
    }
}
