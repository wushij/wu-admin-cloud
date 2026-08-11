package com.wu.cloud.api.pay.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 DTO (跨服务 RPC 传输)
 */
@Data
public class PayOrderDTO implements Serializable {

    private Long id;
    private String payNo;
    private String merchantOrderNo;
    private BigDecimal amount;
    private String channelCode;
    private String status;
    private LocalDateTime createTime;
}
