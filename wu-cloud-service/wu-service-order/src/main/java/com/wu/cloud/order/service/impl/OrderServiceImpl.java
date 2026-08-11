package com.wu.cloud.order.service.impl;

import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.mapper.TradeOrderMapper;
import com.wu.cloud.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public TradeOrderDO createOrder(TradeOrderDO orderDO) {
        if (orderDO.getOrderNo() == null) {
            orderDO.setOrderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        orderDO.setStatus("UNPAID");
        tradeOrderMapper.insert(orderDO);
        return orderDO;
    }

    @Override
    public TradeOrderDO getOrderById(Long id) {
        return tradeOrderMapper.selectById(id);
    }
}
