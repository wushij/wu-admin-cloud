package com.wu.cloud.pay.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付下单请求 VO
 */
@Data
public class PayOrderCreateReqVO implements Serializable {

    private String merchantOrderNo;
    private BigDecimal amount;
    private String channelCode;
    private String subject;
    private String body;
}
