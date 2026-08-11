package com.wu.cloud.api.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 DTO (跨服务 RPC 传输)
 */
@Data
public class TradeOrderDTO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String status;
    private LocalDateTime createTime;
}
