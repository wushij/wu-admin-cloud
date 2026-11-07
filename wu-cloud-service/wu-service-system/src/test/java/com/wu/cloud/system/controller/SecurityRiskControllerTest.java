package com.wu.cloud.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SecurityEventDO;
import com.wu.cloud.system.service.security.AntiBotRiskService;
import com.wu.cloud.system.service.security.RiskLevel;
import com.wu.cloud.system.service.security.RiskSubjectPage;
import com.wu.cloud.system.service.security.SecurityEventService;
import com.wu.cloud.system.vo.RiskSubjectVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全风控 Controller 契约测试。
 *
 * <p>重点校验「前端调用参数 → 服务层参数」的映射是否正确：
 * 前端分页用 pageNo/pageSize，若映射写错会导致列表恒返回第一页或全量。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityRiskController 契约测试")
class SecurityRiskControllerTest {

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private AntiBotRiskService antiBotRiskService;

    @InjectMocks
    private SecurityRiskController controller;

    @Test
    @DisplayName("listEvents：pageNo/pageSize 与过滤条件正确透传给服务层")
    void listEvents_passesParams() {
        Page<SecurityEventDO> page = new Page<>(3, 25, 128);
        page.setRecords(List.of(new SecurityEventDO()));
        when(securityEventService.getPage(anyInt(), anyInt(), any(), any(), any())).thenReturn(page);

        R<PageResult<SecurityEventDO>> result = controller.listEvents(
                new com.wu.cloud.common.core.domain.PageQuery() {{
                    setPageNo(3);
                    setPageSize(25);
                }},
                "10.0.0.1", "UA_BLOCK", "BLOCKED");

        assertTrue(result.isSuccess());
        assertEquals(128L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());

        verify(securityEventService).getPage(3, 25, "10.0.0.1", "UA_BLOCK", "BLOCKED");
    }

    @Test
    @DisplayName("listEvents：分页参数为空时使用默认值 1/10")
    void listEvents_defaultPaging() {
        when(securityEventService.getPage(anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new Page<>(1, 10));

        controller.listEvents(null, null, null, null);

        verify(securityEventService).getPage(1, 10, null, null, null);
    }

    @Test
    @DisplayName("getStats：返回大盘统计")
    void getStats_returnsStats() {
        when(securityEventService.getDashboardStats())
                .thenReturn(Map.of("todayTotal", 12L, "activeRiskCount", 3L));

        R<Map<String, Object>> result = controller.getStats();

        assertTrue(result.isSuccess());
        assertEquals(12L, result.getData().get("todayTotal"));
        assertEquals(3L, result.getData().get("activeRiskCount"));
    }

    @Test
    @DisplayName("getRiskList：返回风险主体列表")
    void getRiskList_returnsSubjects() {
        RiskSubjectVO vo = new RiskSubjectVO();
        vo.setPrincipal("ip:1.2.3.4");
        when(antiBotRiskService.getActiveRiskList()).thenReturn(List.of(vo));

        R<List<RiskSubjectVO>> result = controller.getRiskList();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("ip:1.2.3.4", result.getData().get(0).getPrincipal());
    }

    @Test
    @DisplayName("listRiskSubjects：分页与过滤条件正确透传")
    void listRiskSubjects_passesParams() {
        when(antiBotRiskService.getRiskSubjectPage(anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new RiskSubjectPage(List.of(), 0L, 1, 10));

        controller.listRiskSubjects(
                new com.wu.cloud.common.core.domain.PageQuery() {{
                    setPageNo(2);
                    setPageSize(20);
                }},
                "keyword", 3, Boolean.TRUE);

        verify(antiBotRiskService).getRiskSubjectPage(2, 20, "keyword", 3, Boolean.TRUE);
    }

    @Test
    @DisplayName("recentEvents：limit 缺省为 20")
    void recentEvents_defaultLimit() {
        when(antiBotRiskService.getRecentEvents(anyString(), anyInt())).thenReturn(List.of());

        controller.recentEvents("ip:1.2.3.4", null);

        verify(antiBotRiskService).getRecentEvents("ip:1.2.3.4", 20);
    }

    @Test
    @DisplayName("riskScore：返回分数与等级文案")
    void riskScore_returnsScoreAndLevel() {
        when(antiBotRiskService.getRiskScore("ip:1.2.3.4")).thenReturn(85);
        when(antiBotRiskService.getRiskLevel("ip:1.2.3.4")).thenReturn(RiskLevel.WARNING);

        R<Map<String, Object>> result = controller.riskScore("ip:1.2.3.4");

        assertTrue(result.isSuccess());
        assertEquals(85, result.getData().get("score"));
        assertEquals(RiskLevel.WARNING.getCode(), result.getData().get("level"));
        assertEquals("警告", result.getData().get("levelText"));
    }

    @Test
    @DisplayName("block：时长与原因透传给服务层")
    void block_passesParams() {
        R<Boolean> result = controller.block("user:42", 7200L, "恶意刷接口");

        assertTrue(result.isSuccess());
        verify(antiBotRiskService).blockPrincipal("user:42", 7200L, "恶意刷接口");
    }

    @Test
    @DisplayName("block：时长缺省为 3600 秒")
    void block_defaultDuration() {
        controller.block("user:42", null, null);

        verify(antiBotRiskService).blockPrincipal("user:42", 3600L, null);
    }

    @Test
    @DisplayName("unblock：resetRisk 缺省为 true")
    void unblock_defaultResetRisk() {
        R<Boolean> result = controller.unblock("ip:1.2.3.4", null);

        assertTrue(result.isSuccess());
        verify(antiBotRiskService).unblockPrincipal("ip:1.2.3.4", true);
    }

    @Test
    @DisplayName("unblock：显式传 false 时不清零风险分")
    void unblock_keepRiskScore() {
        controller.unblock("ip:1.2.3.4", false);

        verify(antiBotRiskService).unblockPrincipal("ip:1.2.3.4", false);
    }

    @Test
    @DisplayName("delete：删除指定安全事件")
    void delete_delegatesToService() {
        R<Boolean> result = controller.delete(99L);

        assertTrue(result.isSuccess());
        verify(securityEventService).delete(99L);
    }

    @Test
    @DisplayName("clear：清空安全事件日志")
    void clear_delegatesToService() {
        R<Boolean> result = controller.clear();

        assertTrue(result.isSuccess());
        verify(securityEventService).clear();
    }
}
