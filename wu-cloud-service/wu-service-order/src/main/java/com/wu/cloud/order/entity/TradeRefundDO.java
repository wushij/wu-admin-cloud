package com.wu.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易退款单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_refund")
public class TradeRefundDO extends BaseEntity {

    /** 退款单号 */
    private String refundNo;
    /** 订单 ID */
    private Long orderId;
    /** 订单编号 */
    private String orderNo;
    /** 用户 ID */
    private Long userId;
    /** 退款金额 */
    private BigDecimal amount;
    /** 退款原因 */
    private String reason;
    /** 状态: PENDING/APPROVED/REJECTED/SUCCESS/FAILED */
    private String status;
    /** 审核人 ID */
    private Long auditorUserId;
    /** 审核备注 */
    private String auditRemark;
    /** 退款时间 */
    private LocalDateTime refundTime;
    /** 第三方退款单号（支付渠道返回） */
    private String transactionId;
}
