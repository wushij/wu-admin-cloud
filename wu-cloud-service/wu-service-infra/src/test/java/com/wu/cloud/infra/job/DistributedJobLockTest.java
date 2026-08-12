package com.wu.cloud.infra.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedJobLock 单元测试")
class DistributedJobLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private DistributedJobLock jobLock;

    @Test
    @DisplayName("tryLock：setIfAbsent 成功返回 true")
    void tryLock_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("job:lock:outboxCompensate"), eq("1"), eq(600L), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.TRUE);
        assertTrue(jobLock.tryLock("outboxCompensate"));
    }

    @Test
    @DisplayName("tryLock：已被占用返回 false")
    void tryLock_held() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.FALSE);
        assertFalse(jobLock.tryLock("outboxCompensate", 120L));
    }

    @Test
    @DisplayName("unlock：删除锁 key")
    void unlock_deletes() {
        jobLock.unlock("outboxCompensate");
        verify(redisTemplate).delete("job:lock:outboxCompensate");
    }
}
