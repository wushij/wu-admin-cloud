package com.wu.cloud.admin.service.impl;

import com.wu.cloud.admin.vo.DashboardStatsVO;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                remoteStatsService, remoteTicketService, remoteOnlineUserService);
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
        when(remoteStatsService.getRecentLogins(anyInt())).thenThrow(new RuntimeException("logins down"));
        when(remoteTicketService.getNoticeUnreadCount(anyLong())).thenThrow(new RuntimeException("notice down"));
        when(remoteTicketService.getDashboardStats()).thenThrow(new RuntimeException("ticket down"));
        when(remoteOnlineUserService.countOnlineUsers()).thenThrow(new RuntimeException("online down"));
        doThrow(new RuntimeException("touch fail")).when(remoteOnlineUserService).touchLastAccess(anyLong());

        DashboardStatsVO vo = dashboardService.getDashboardStats();

        assertNotNull(vo);
        assertEquals(0L, vo.getUserCount() == null ? 0L : vo.getUserCount());
        assertNotNull(vo.getRecentLogins());
        assertTrue(vo.getRecentLogins().isEmpty());
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
                "yesterdayVisits", 2
        )));
        RecentLoginDTO login = new RecentLoginDTO();
        login.setUserId(9L);
        login.setUsername("bob");
        login.setLoginTime(LocalDateTime.of(2026, 8, 12, 9, 0, 0));
        when(remoteStatsService.getRecentLogins(anyInt())).thenReturn(R.ok(List.of(login)));
        when(remoteTicketService.getNoticeUnreadCount(1L)).thenReturn(R.ok(4L));
        TicketDashboardDTO ticket = new TicketDashboardDTO();
        ticket.setTicketOpenCount(7L);
        ticket.setTicketOverdueCount(1L);
        ticket.setApprovalPendingCount(2L);
        when(remoteTicketService.getDashboardStats()).thenReturn(R.ok(ticket));
        when(remoteOnlineUserService.countOnlineUsers()).thenReturn(R.ok(6));

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
        assertEquals(1, vo.getRecentLogins().size());
        assertEquals("bob", vo.getRecentLogins().get(0).getUsername());
        verify(remoteOnlineUserService).touchLastAccess(1L);
    }
}

