package com.wu.cloud.ai.service.impl;

import com.wu.cloud.common.config.DynamicConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiChatQuotaGuard 单元测试")
class AiChatQuotaGuardTest {

    @Mock
    private DynamicConfigHelper dynamicConfigHelper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RAtomicLong atomicLong;

    @InjectMocks
    private AiChatQuotaGuard guard;

    @Test
    @DisplayName("check：userId 非法直接放行")
    void check_invalidUser_pass() {
        assertNull(guard.check(null));
        assertNull(guard.check(0L));
        verifyNoInteractions(redissonClient);
    }

    @Test
    @DisplayName("check：分钟限流超限拒绝")
    void check_minuteLimitExceeded() {
        when(dynamicConfigHelper.getAiChatPerUserMinute()).thenReturn(2);
        when(dynamicConfigHelper.getAiTokensPerUserDaily()).thenReturn(0L);
        when(redissonClient.getAtomicLong(startsWith("ai:rl:chat:"))).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(3L);

        String msg = guard.check(9L);
        assertNotNull(msg);
        assertTrue(msg.contains("频繁"));
    }

    @Test
    @DisplayName("check：日 token 配额用尽拒绝")
    void check_dailyQuotaExceeded() {
        when(dynamicConfigHelper.getAiChatPerUserMinute()).thenReturn(0);
        when(dynamicConfigHelper.getAiTokensPerUserDaily()).thenReturn(100L);
        when(redissonClient.getAtomicLong(startsWith("ai:quota:tokens:"))).thenReturn(atomicLong);
        when(atomicLong.get()).thenReturn(100L);

        String msg = guard.check(9L);
        assertNotNull(msg);
        assertTrue(msg.contains("上限"));
    }

    @Test
    @DisplayName("check：Redis 异常 fail-open")
    void check_redisError_failOpen() {
        when(dynamicConfigHelper.getAiChatPerUserMinute()).thenReturn(5);
        when(redissonClient.getAtomicLong(anyString())).thenThrow(new RuntimeException("redis down"));

        assertNull(guard.check(9L));
    }

    @Test
    @DisplayName("recordUsage：回写 token 用量")
    void recordUsage_addsTokens() {
        when(dynamicConfigHelper.getAiTokensPerUserDaily()).thenReturn(1000L);
        when(redissonClient.getAtomicLong(startsWith("ai:quota:tokens:"))).thenReturn(atomicLong);
        when(atomicLong.remainTimeToLive()).thenReturn(-1L);

        guard.recordUsage(9L, 50L);

        verify(atomicLong).addAndGet(50L);
        verify(atomicLong).expire(Duration.ofHours(48));
    }

    @Test
    @DisplayName("recordUsage：配额关闭时不写入")
    void recordUsage_quotaDisabled_noop() {
        when(dynamicConfigHelper.getAiTokensPerUserDaily()).thenReturn(0L);

        guard.recordUsage(9L, 50L);

        verifyNoInteractions(redissonClient);
    }
}
