package com.wu.cloud.infra.service.monitor.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OnlineUserServiceImpl 单元测试")
class OnlineUserServiceImplTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private TokenService tokenService;
    @Mock
    private RemoteUserService remoteUserService;
    @Mock
    private RBucket<String> detailBucket;
    @Mock
    private RBucket<String> activeBucket;

    @InjectMocks
    private OnlineUserServiceImpl onlineUserService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(tokenService.tokenTimeoutMinutes()).thenReturn(30L);
    }

    @Test
    @DisplayName("recordLoginSession：空参忽略，正常写入 detail/active")
    void recordLoginSession() {
        onlineUserService.recordLoginSession(null);
        verifyNoInteractions(redissonClient);

        when(redissonClient.<String>getBucket("monitor:online:detail:7")).thenReturn(detailBucket);
        when(redissonClient.<String>getBucket("dashboard:online:user:7")).thenReturn(activeBucket);
        when(remoteUserService.getUserById(7L)).thenReturn(null);

        RecordLoginSessionDTO dto = new RecordLoginSessionDTO();
        dto.setUserId(7L);
        dto.setUsername("bob");
        dto.setNickname("鲍勃");
        dto.setClientIp("127.0.0.1");
        dto.setUserAgent("Mozilla/5.0 Chrome/120.0");

        onlineUserService.recordLoginSession(dto);

        verify(detailBucket).set(argThat(json -> json.contains("bob")), eq(Duration.ofDays(1)));
        verify(activeBucket).set(any(), any(Duration.class));
    }

    @Test
    @DisplayName("touchLastAccess：45s 内节流")
    void touch_throttled() {
        when(redissonClient.<String>getBucket(anyString())).thenReturn(detailBucket);
        when(redissonClient.<String>getBucket(startsWith("dashboard:online:user:"))).thenReturn(activeBucket);
        when(detailBucket.get()).thenReturn(null);

        // 先写入 throttle cache
        when(redissonClient.<String>getBucket("monitor:online:detail:8")).thenReturn(detailBucket);
        when(redissonClient.<String>getBucket("dashboard:online:user:8")).thenReturn(activeBucket);
        RecordLoginSessionDTO dto = new RecordLoginSessionDTO();
        dto.setUserId(8L);
        dto.setUsername("c");
        onlineUserService.recordLoginSession(dto);
        clearInvocations((Object) activeBucket);
        clearInvocations((Object) detailBucket);

        onlineUserService.touchLastAccess(8L);
        verify(activeBucket, never()).set(any(), any(Duration.class));
    }

    @Test
    @DisplayName("forceLogout：踢人并清理 Redis / token")
    void forceLogout_cleans() {
        when(redissonClient.<String>getBucket("monitor:online:detail:9")).thenReturn(detailBucket);
        when(redissonClient.<String>getBucket("dashboard:online:user:9")).thenReturn(activeBucket);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            onlineUserService.forceLogout(9L);
            stp.verify(() -> StpUtil.kickout(9L));
            stp.verify(() -> StpUtil.logout(9L));
        }
        verify(tokenService).removeToken(9L);
        verify(detailBucket).delete();
        verify(activeBucket).delete();
    }
}
