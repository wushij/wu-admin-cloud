package com.wu.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.order.entity.StockDO;
import com.wu.cloud.order.mapper.StockMapper;
import com.wu.cloud.order.service.StockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，直接注入 Mapper）
 */
@Service
public class StockServiceImpl implements StockService {

    @Resource
    private StockMapper stockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String skuCode, Integer quantity) {
        if (skuCode == null || quantity == null || quantity <= 0) {
            throw new BusinessException("扣减库存参数非法");
        }
        StockDO stock = getStock(skuCode);
        if (stock == null) {
            throw new BusinessException(404, "库存记录不存在: " + skuCode);
        }
        if (stock.getAvailableStock() == null || stock.getAvailableStock() < quantity) {
            throw new BusinessException("可用库存不足: " + skuCode);
        }
        // 乐观锁：通过 available_stock >= quantity 条件保证并发安全
        LambdaUpdateWrapper<StockDO> wrapper = new LambdaUpdateWrapper<StockDO>()
                .eq(StockDO::getSkuCode, skuCode)
                .ge(StockDO::getAvailableStock, quantity)
                .setSql("locked_stock = locked_stock + " + quantity)
                .setSql("available_stock = available_stock - " + quantity);
        int rows = stockMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new BusinessException("库存扣减失败，可能存在并发冲突: " + skuCode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnStock(String skuCode, Integer quantity) {
        if (skuCode == null || quantity == null || quantity <= 0) {
            throw new BusinessException("归还库存参数非法");
        }
        StockDO stock = getStock(skuCode);
        if (stock == null) {
            throw new BusinessException(404, "库存记录不存在: " + skuCode);
        }
        if (stock.getLockedStock() == null || stock.getLockedStock() < quantity) {
            throw new BusinessException("锁定库存不足，无法归还: " + skuCode);
        }
        LambdaUpdateWrapper<StockDO> wrapper = new LambdaUpdateWrapper<StockDO>()
                .eq(StockDO::getSkuCode, skuCode)
                .ge(StockDO::getLockedStock, quantity)
                .setSql("locked_stock = locked_stock - " + quantity)
                .setSql("available_stock = available_stock + " + quantity);
        int rows = stockMapper.update(null, wrapper);
        if (rows <= 0) {
            throw new BusinessException("库存归还失败，可能存在并发冲突: " + skuCode);
        }
    }

    @Override
    public StockDO getStock(String skuCode) {
        return stockMapper.selectOne(new LambdaQueryWrapper<StockDO>().eq(StockDO::getSkuCode, skuCode));
    }

    @Override
    public List<StockDO> listStock() {
        return stockMapper.selectList(new LambdaQueryWrapper<StockDO>().orderByAsc(StockDO::getSkuCode));
    }
}
