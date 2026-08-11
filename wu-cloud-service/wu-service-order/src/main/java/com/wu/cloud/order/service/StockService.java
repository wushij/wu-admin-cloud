package com.wu.cloud.order.service;

import com.wu.cloud.order.entity.StockDO;

import java.util.List;

/**
 * 库存 Service
 */
public interface StockService {

    /** 扣减库存（锁定） */
    void deduct(String skuCode, Integer quantity);

    /** 归还库存（解锁） */
    void returnStock(String skuCode, Integer quantity);

    /** 查询库存 */
    StockDO getStock(String skuCode);

    /** 库存列表 */
    List<StockDO> listStock();
}
