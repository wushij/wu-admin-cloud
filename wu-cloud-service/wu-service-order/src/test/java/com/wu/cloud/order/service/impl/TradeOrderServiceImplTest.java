package com.wu.cloud.order.service.impl;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.order.entity.OrderItemDO;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.mapper.OrderItemMapper;
import com.wu.cloud.order.mapper.TradeOrderMapper;
import com.wu.cloud.order.service.StockService;
import com.wu.cloud.order.testsupport.MybatisLambdaTestBase;
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

import java.math.BigDecimal;
import java.util.List;

import static com.wu.cloud.order.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TradeOrderServiceImpl 单元测试")
class TradeOrderServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private StockService stockService;

    @InjectMocks
    private TradeOrderServiceImpl tradeOrderService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder().userId(7L).username("buyer").build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("create：落库 PENDING 并扣减库存")
    void create_deductsStock() {
        when(tradeOrderMapper.insert(any(TradeOrderDO.class))).thenAnswer(inv -> {
            TradeOrderDO o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        });
        when(orderItemMapper.insert(any(OrderItemDO.class))).thenReturn(1);

        TradeOrderDO order = new TradeOrderDO();
        order.setSubject("测试商品");
        order.setTotalAmount(new BigDecimal("19.9"));
        OrderItemDO item = new OrderItemDO();
        item.setSkuCode("SKU-A");
        item.setQuantity(2);
        order.setItems(List.of(item));

        Long id = tradeOrderService.create(order);

        assertEquals(100L, id);
        assertEquals(7L, order.getUserId());
        assertEquals("PENDING", order.getStatus());
        assertNotNull(order.getOrderNo());
        verify(stockService).deduct("SKU-A", 2);
        verify(orderItemMapper).insert(any(OrderItemDO.class));
    }

    @Test
    @DisplayName("markPaidByOrderNo：PENDING→PAID")
    void markPaid_pendingToPaid() {
        TradeOrderDO order = order(1L, "T001", "PENDING");
        when(tradeOrderMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(order);
        when(tradeOrderMapper.updateById(any(TradeOrderDO.class))).thenReturn(1);

        assertTrue(tradeOrderService.markPaidByOrderNo("T001", "WECHAT"));
        assertEquals("PAID", order.getStatus());
        assertEquals("WECHAT", order.getPayType());
        assertNotNull(order.getPayTime());
    }

    @Test
    @DisplayName("markPaidByOrderNo：已支付幂等返回 true")
    void markPaid_alreadyPaid_idempotent() {
        TradeOrderDO order = order(1L, "T001", "PAID");
        when(tradeOrderMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(order);

        assertTrue(tradeOrderService.markPaidByOrderNo("T001", "WECHAT"));
        verify(tradeOrderMapper, never()).updateById(any(TradeOrderDO.class));
    }

    @Test
    @DisplayName("markPaidByOrderNo：已取消不强制流转")
    void markPaid_cancelled_returnsFalse() {
        TradeOrderDO order = order(1L, "T001", "CANCELLED");
        when(tradeOrderMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(order);

        assertFalse(tradeOrderService.markPaidByOrderNo("T001", "WECHAT"));
        verify(tradeOrderMapper, never()).updateById(any(TradeOrderDO.class));
    }

    @Test
    @DisplayName("cancel：归还库存")
    void cancel_returnsStock() {
        TradeOrderDO order = order(9L, "T009", "PENDING");
        when(tradeOrderMapper.selectById(9L)).thenReturn(order);
        when(tradeOrderMapper.updateById(any(TradeOrderDO.class))).thenReturn(1);

        OrderItemDO item = new OrderItemDO();
        item.setSkuCode("SKU-A");
        item.setQuantity(2);
        when(orderItemMapper.selectList(anyLambdaQueryWrapper())).thenReturn(List.of(item));

        tradeOrderService.cancel(9L);

        assertEquals("CANCELLED", order.getStatus());
        verify(stockService).returnStock("SKU-A", 2);
    }

    @Test
    @DisplayName("transition：非法流转拒绝")
    void transition_illegal_throws() {
        TradeOrderDO order = order(1L, "T001", "PENDING");
        when(tradeOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tradeOrderService.transition(1L, "SHIPPED"));
        assertTrue(ex.getMessage().contains("不允许"));
    }

    private static TradeOrderDO order(Long id, String orderNo, String status) {
        TradeOrderDO o = new TradeOrderDO();
        o.setId(id);
        o.setOrderNo(orderNo);
        o.setStatus(status);
        return o;
    }
}
