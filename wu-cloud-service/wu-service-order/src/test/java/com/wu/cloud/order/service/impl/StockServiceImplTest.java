package com.wu.cloud.order.service.impl;

import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.order.entity.StockDO;
import com.wu.cloud.order.mapper.StockMapper;
import com.wu.cloud.order.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.wu.cloud.order.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper;
import static com.wu.cloud.order.testsupport.MybatisMockMatchers.anyLambdaUpdateWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockServiceImpl 单元测试")
class StockServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private StockMapper stockMapper;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    @DisplayName("deduct：参数非法拒绝")
    void deduct_illegalArgs_throws() {
        assertThrows(BusinessException.class, () -> stockService.deduct(null, 1));
        assertThrows(BusinessException.class, () -> stockService.deduct("SKU1", 0));
        assertThrows(BusinessException.class, () -> stockService.deduct("SKU1", -1));
    }

    @Test
    @DisplayName("deduct：库存不存在拒绝")
    void deduct_missing_throws() {
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> stockService.deduct("SKU1", 1));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("deduct：可用库存不足拒绝")
    void deduct_insufficient_throws() {
        StockDO stock = stock("SKU1", 2, 0);
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(stock);

        BusinessException ex = assertThrows(BusinessException.class, () -> stockService.deduct("SKU1", 5));
        assertTrue(ex.getMessage().contains("可用库存不足"));
        verify(stockMapper, never()).update(isNull(), anyLambdaUpdateWrapper());
    }

    @Test
    @DisplayName("deduct：乐观锁更新成功")
    void deduct_success() {
        StockDO stock = stock("SKU1", 10, 0);
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(stock);
        when(stockMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        assertDoesNotThrow(() -> stockService.deduct("SKU1", 3));
        verify(stockMapper).update(isNull(), anyLambdaUpdateWrapper());
    }

    @Test
    @DisplayName("deduct：并发冲突拒绝")
    void deduct_concurrentConflict_throws() {
        StockDO stock = stock("SKU1", 10, 0);
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(stock);
        when(stockMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> stockService.deduct("SKU1", 3));
        assertTrue(ex.getMessage().contains("并发冲突"));
    }

    @Test
    @DisplayName("returnStock：锁定库存不足拒绝")
    void returnStock_insufficientLocked_throws() {
        StockDO stock = stock("SKU1", 8, 1);
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(stock);

        BusinessException ex = assertThrows(BusinessException.class, () -> stockService.returnStock("SKU1", 3));
        assertTrue(ex.getMessage().contains("锁定库存不足"));
    }

    @Test
    @DisplayName("returnStock：归还成功")
    void returnStock_success() {
        StockDO stock = stock("SKU1", 7, 3);
        when(stockMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(stock);
        when(stockMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        assertDoesNotThrow(() -> stockService.returnStock("SKU1", 2));
        verify(stockMapper).update(isNull(), anyLambdaUpdateWrapper());
    }

    private static StockDO stock(String sku, int available, int locked) {
        StockDO s = new StockDO();
        s.setSkuCode(sku);
        s.setAvailableStock(available);
        s.setLockedStock(locked);
        s.setTotalStock(available + locked);
        return s;
    }
}
