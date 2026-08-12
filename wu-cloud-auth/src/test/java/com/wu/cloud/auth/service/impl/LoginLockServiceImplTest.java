package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.support.AuthRedisKeys;
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
@DisplayName("LoginLockServiceImpl 单元测试")
class LoginLockServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AuthConfigLoader authConfigLoader;

    @InjectMocks
    private LoginLockServiceImpl loginLockService;

    @Test
    @DisplayName("assertNotLocked：TTL>0 时抛出锁定异常")
    void assertNotLocked_whenLocked_throws() {
        when(redisTemplate.getExpire(AuthRedisKeys.loginLock("admin"))).thenReturn(120L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginLockService.assertNotLocked("admin"));
        assertTrue(ex.getMessage().contains("账号已被锁定"));
    }

    @Test
    @DisplayName("assertNotLocked：未锁定时放行")
    void assertNotLocked_whenNotLocked_ok() {
        when(redisTemplate.getExpire(AuthRedisKeys.loginLock("admin"))).thenReturn(-2L);
        assertDoesNotThrow(() -> loginLockService.assertNotLocked("admin"));
    }

    @Test
    @DisplayName("recordFailure：未达上限返回剩余次数")
    void recordFailure_belowMax_returnsRemaining() {
        when(authConfigLoader.getMaxRetryCount()).thenReturn(5);
        when(authConfigLoader.getLockTimeSeconds()).thenReturn(300L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(AuthRedisKeys.loginFail("admin"))).thenReturn(2L);

        assertEquals(3, loginLockService.recordFailure("admin"));
        verify(redisTemplate, never()).expire(eq(AuthRedisKeys.loginFail("admin")), any(Duration.class));
        verify(valueOps, never()).set(eq(AuthRedisKeys.loginLock("admin")), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("recordFailure：首次失败设置过期")
    void recordFailure_firstFailure_setsExpire() {
        when(authConfigLoader.getMaxRetryCount()).thenReturn(5);
        when(authConfigLoader.getLockTimeSeconds()).thenReturn(300L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(AuthRedisKeys.loginFail("admin"))).thenReturn(1L);

        assertEquals(4, loginLockService.recordFailure("admin"));
        verify(redisTemplate).expire(AuthRedisKeys.loginFail("admin"), Duration.ofSeconds(300L));
    }

    @Test
    @DisplayName("recordFailure：达上限写入锁并清零失败计数")
    void recordFailure_reachesMax_locksAccount() {
        when(authConfigLoader.getMaxRetryCount()).thenReturn(5);
        when(authConfigLoader.getLockTimeSeconds()).thenReturn(300L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(AuthRedisKeys.loginFail("admin"))).thenReturn(5L);

        assertEquals(0, loginLockService.recordFailure("admin"));
        verify(valueOps).set(AuthRedisKeys.loginLock("admin"), "1", Duration.ofSeconds(300L));
        verify(redisTemplate).delete(AuthRedisKeys.loginFail("admin"));
    }

    @Test
    @DisplayName("clearFailure：删除失败与锁定 key")
    void clearFailure_deletesKeys() {
        loginLockService.clearFailure("admin");
        verify(redisTemplate).delete(AuthRedisKeys.loginFail("admin"));
        verify(redisTemplate).delete(AuthRedisKeys.loginLock("admin"));
    }
}
