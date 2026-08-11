package com.wu.cloud.order.service;

import com.wu.cloud.order.entity.TradeOrderDO;

public interface OrderService {
    TradeOrderDO createOrder(TradeOrderDO orderDO);
    TradeOrderDO getOrderById(Long id);
}
