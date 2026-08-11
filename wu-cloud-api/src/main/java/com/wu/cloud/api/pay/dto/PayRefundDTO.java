package com.wu.cloud.api.pay.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单 DTO (跨服务 RPC 传输)
 */
@Data
public class PayRefundDTO implements Serializable {

    private Long id;
    /** 退款单号 */
    private String refundNo;
    /** 原支付订单号 */
    private String orderNo;
    /** 支付类型: ALIPAY / WECHAT / BALANCE */
    private String payType;
    /** 退款金额 */
    private BigDecimal amount;
    /** 状态: PENDING/APPROVED/REJECTED/PROCESSING/SUCCESS/FAILED */
    private String status;
    /** 第三方退款单号 */
    private String transactionId;
    private LocalDateTime createTime;
}
