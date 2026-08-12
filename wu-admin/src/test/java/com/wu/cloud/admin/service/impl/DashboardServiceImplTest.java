package com.wu.cloud.admin.service.impl;

import com.wu.cloud.admin.vo.DashboardStatsVO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.api.infra.feign.RemoteJobService;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.message.feign.RemoteMessageService;
import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.api.system.feign.RemoteStatsService;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardServiceImpl 降级单测")
class DashboardServiceImplTest {

    @Mock
    private RemoteStatsService remoteStatsService;
    @Mock
    private RemoteTicketService remoteTicketService;
    @Mock
    private RemoteOnlineUserService remoteOnlineUserService;
    @Mock
    private RemoteFileService remoteFileService;
    @Mock
    private RemoteMessageService remoteMessageService;
    @Mock
    private RemoteJobService remoteJobService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        dashboardService = new DashboardServiceImpl(
                remoteStatsService, remoteTicketService, remoteOnlineUserService,
                remoteFileService, remoteMessageService, remoteJobService, stringRedisTemplate);
        UserContextHolder.set(UserContext.builder().userId(1L).username("alice").build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("下游全部抛异常时仍返回可渲染默认值")
    void allDownstreamsFail_degradesToDefaults() {
        when(remoteStatsService.getDashboardStats()).thenThrow(new RuntimeException("stats down"));
        when(remoteMessageService.getChatUnreadCount(anyLong())).thenThrow(new RuntimeException("chat down"));
        when(remoteTicketService.getDashboardStats(anyLong())).thenThrow(new RuntimeException("ticket down"));
        when(remoteOnlineUserService.countOnlineUsers()).thenThrow(new RuntimeException("online down"));
        when(remoteFileService.countFiles()).thenThrow(new RuntimeException("file down"));
        when(remoteJobService.overview()).thenThrow(new RuntimeException("job down"));
        doThrow(new RuntimeException("touch fail")).when(remoteOnlineUserService).touchLastAccess(anyLong());

        DashboardStatsVO vo = dashboardService.getDashboardStats();

        assertNotNull(vo);
        assertEquals(0L, vo.getUserCount() == null ? 0L : vo.getUserCount());
        assertEquals(0L, vo.getFileCount() == null ? 0L : vo.getFileCount());
    }

    @Test
    @DisplayName("下游正常返回时映射统计字段")
    void happyPath_mapsFields() {
        when(remoteStatsService.getDashboardStats()).thenReturn(R.ok(Map.of(
                "platformName", "WU Cloud",
                "platformSubtitle", "ops",
                "userCount", 12,
                "roleCount", 3,
                "todayVisits", 5,
                "yesterdayVisits", 2,
                "configGroupCount", 11
        )));
        when(remoteMessageService.getChatUnreadCount(1L)).thenReturn(R.ok(4L));
        TicketDashboardDTO ticket = new TicketDashboardDTO();
        ticket.setTicketOpenCount(7L);
        ticket.setTicketOverdueCount(1L);
        ticket.setApprovalPendingCount(2L);
        when(remoteTicketService.getDashboardStats(1L)).thenReturn(R.ok(ticket));
        when(remoteOnlineUserService.countOnlineUsers()).thenReturn(R.ok(6));
        when(remoteFileService.countFiles()).thenReturn(R.ok(9L));
        when(remoteJobService.overview()).thenReturn(R.ok(Map.of(
                "totalJobs", 5,
                "runningJobs", 2,
                "pausedJobs", 3
        )));

        DashboardStatsVO vo = dashboardService.getDashboardStats();

        assertEquals("WU Cloud", vo.getPlatformName());
        assertEquals("ops", vo.getPlatformSubtitle());
        assertEquals(12L, vo.getUserCount());
        assertEquals(3L, vo.getRoleCount());
        assertEquals(7L, vo.getVisitCount());
        assertEquals(6L, vo.getOnlineCount());
        assertEquals(4L, vo.getChatUnreadCount());
        assertEquals(7L, vo.getTicketOpenCount());
        assertEquals(1L, vo.getTicketOverdueCount());
        assertEquals(2L, vo.getApprovalPendingCount());
        assertEquals(9L, vo.getFileCount());
        assertEquals(5L, vo.getJobTotalCount());
        assertEquals(2L, vo.getJobRunningCount());
        assertEquals(3L, vo.getJobPausedCount());
        assertEquals(11L, vo.getConfigGroupCount());
        verify(remoteTicketService).getDashboardStats(1L);
        verify(remoteMessageService).getChatUnreadCount(1L);
        verify(valueOperations).set(anyString(), anyString(), eq(60L), any());
    }

    @Test
    @DisplayName("命中缓存时不再打下游")
    void cacheHit_skipsDownstreams() {
        when(valueOperations.get(anyString())).thenReturn(
                "{\"platformName\":\"Cached\",\"userCount\":99,\"onlineCount\":1}");

        DashboardStatsVO vo = dashboardService.getDashboardStats();

        assertEquals("Cached", vo.getPlatformName());
        assertEquals(99L, vo.getUserCount());
        verify(remoteStatsService, never()).getDashboardStats();
    }

    @Test
    @DisplayName("最近登录独立接口仍可用")
    void recentLogins_ok() {
        RecentLoginDTO login = new RecentLoginDTO();
        login.setUserId(9L);
        login.setUsername("bob");
        login.setLoginTime(LocalDateTime.of(2026, 8, 12, 9, 0, 0));
        when(remoteStatsService.getRecentLogins(anyInt())).thenReturn(R.ok(List.of(login)));

        assertEquals(1, dashboardService.getRecentLogins(8).size());
        assertEquals("bob", dashboardService.getRecentLogins(8).get(0).getUsername());
    }
}
