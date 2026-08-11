package com.wu.cloud.order.convert;

import com.wu.cloud.api.order.dto.TradeOrderDTO;
import com.wu.cloud.order.entity.TradeOrderDO;

public class OrderConvert {

    public static TradeOrderDTO convertToDTO(TradeOrderDO orderDO) {
        if (orderDO == null) {
            return null;
        }
        TradeOrderDTO dto = new TradeOrderDTO();
        dto.setId(orderDO.getId());
        dto.setOrderNo(orderDO.getOrderNo());
        dto.setUserId(orderDO.getUserId());
        dto.setTotalAmount(orderDO.getTotalAmount());
        dto.setPayAmount(orderDO.getPayAmount());
        dto.setStatus(orderDO.getStatus());
        dto.setCreateTime(orderDO.getCreateTime());
        return dto;
    }
}
