package com.wu.cloud.pay.service.impl;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.entity.PayRefundDO;
import com.wu.cloud.pay.mapper.PayOrderMapper;
import com.wu.cloud.pay.mapper.PayRefundMapper;
import com.wu.cloud.pay.service.PayService;
import com.wu.cloud.pay.service.PayServiceFactory;
import com.wu.cloud.pay.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static com.wu.cloud.pay.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PayRefundServiceImpl 单元测试")
class PayRefundServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private PayRefundMapper payRefundMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private PayServiceFactory payServiceFactory;
    @Mock
    private PayService payService;

    @InjectMocks
    private PayRefundServiceImpl payRefundService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(payRefundService, "baseMapper", payRefundMapper);
        UserContextHolder.set(UserContext.builder().userId(1L).username("alice").build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("apply：非已支付订单拒绝")
    void apply_notPaid_throws() {
        PayOrderDO order = payOrder(10L, "P001", "PENDING");
        when(payOrderMapper.selectById(10L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> payRefundService.apply(10L, "不想要了"));
        assertTrue(ex.getMessage().contains("已支付"));
    }

    @Test
    @DisplayName("apply：已支付创建 PENDING 退款单")
    void apply_paid_createsPending() {
        PayOrderDO order = payOrder(10L, "P001", "PAID");
        order.setPayType("WECHAT");
        order.setAmount(new BigDecimal("88.00"));
        when(payOrderMapper.selectById(10L)).thenReturn(order);
        when(payRefundMapper.insert(any(PayRefundDO.class))).thenAnswer(inv -> {
            PayRefundDO r = inv.getArgument(0);
            r.setId(200L);
            return 1;
        });

        Long id = payRefundService.apply(10L, "质量问题");

        assertEquals(200L, id);
        verify(payRefundMapper).insert(argThat((PayRefundDO r) ->
                "PENDING".equals(r.getStatus())
                        && "P001".equals(r.getOrderNo())
                        && "质量问题".equals(r.getReason())));
    }

    @Test
    @DisplayName("execute：同步 SUCCESS 直接终态")
    void execute_syncSuccess() {
        PayRefundDO refund = refund(20L, "R001", "APPROVED");
        when(payRefundMapper.selectById(20L)).thenReturn(refund);
        when(payServiceFactory.getService("WECHAT")).thenReturn(payService);
        when(payService.refund(eq("P001"), eq("R001"), any())).thenReturn(Map.of(
                "refundId", "wx-refund-1",
                "refundStatus", "SUCCESS"));
        when(payRefundMapper.updateById(any(PayRefundDO.class))).thenReturn(1);

        payRefundService.execute(20L);

        assertEquals("SUCCESS", refund.getStatus());
        assertEquals("wx-refund-1", refund.getTransactionId());
        assertNotNull(refund.getRefundTime());
    }

    @Test
    @DisplayName("execute：异步 PROCESSING 等待回调")
    void execute_asyncProcessing() {
        PayRefundDO refund = refund(20L, "R001", "APPROVED");
        when(payRefundMapper.selectById(20L)).thenReturn(refund);
        when(payServiceFactory.getService("WECHAT")).thenReturn(payService);
        when(payService.refund(eq("P001"), eq("R001"), any())).thenReturn(Map.of(
                "refundId", "wx-refund-2",
                "refundStatus", "PROCESSING"));
        when(payRefundMapper.updateById(any(PayRefundDO.class))).thenReturn(1);

        payRefundService.execute(20L);

        assertEquals("PROCESSING", refund.getStatus());
    }

    @Test
    @DisplayName("handleRefundNotify：终态幂等")
    void handleRefundNotify_terminal_idempotent() {
        PayRefundDO refund = refund(20L, "R001", "SUCCESS");
        when(payRefundMapper.selectOne(anyLambdaQueryWrapper(), anyBoolean())).thenReturn(refund);

        payRefundService.handleRefundNotify("R001", "txn-x", true);

        verify(payRefundMapper, never()).updateById(any(PayRefundDO.class));
    }

    @Test
    @DisplayName("handleRefundNotify：PROCESSING→SUCCESS")
    void handleRefundNotify_processingToSuccess() {
        PayRefundDO refund = refund(20L, "R001", "PROCESSING");
        when(payRefundMapper.selectOne(anyLambdaQueryWrapper(), anyBoolean())).thenReturn(refund);
        when(payRefundMapper.updateById(any(PayRefundDO.class))).thenReturn(1);

        payRefundService.handleRefundNotify("R001", "txn-ok", true);

        assertEquals("SUCCESS", refund.getStatus());
        assertEquals("txn-ok", refund.getTransactionId());
        assertNotNull(refund.getRefundTime());
    }

    @Test
    @DisplayName("handleRefundNotify：失败置 FAILED")
    void handleRefundNotify_failed() {
        PayRefundDO refund = refund(20L, "R001", "PROCESSING");
        when(payRefundMapper.selectOne(anyLambdaQueryWrapper(), anyBoolean())).thenReturn(refund);
        when(payRefundMapper.updateById(any(PayRefundDO.class))).thenReturn(1);

        payRefundService.handleRefundNotify("R001", "txn-fail", false);

        assertEquals("FAILED", refund.getStatus());
    }

    private static PayOrderDO payOrder(Long id, String orderNo, String status) {
        PayOrderDO o = new PayOrderDO();
        o.setId(id);
        o.setOrderNo(orderNo);
        o.setStatus(status);
        return o;
    }

    private static PayRefundDO refund(Long id, String refundNo, String status) {
        PayRefundDO r = new PayRefundDO();
        r.setId(id);
        r.setRefundNo(refundNo);
        r.setOrderNo("P001");
        r.setPayType("WECHAT");
        r.setAmount(new BigDecimal("10.00"));
        r.setStatus(status);
        return r;
    }
}
