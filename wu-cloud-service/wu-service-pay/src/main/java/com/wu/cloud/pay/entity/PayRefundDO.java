package com.wu.cloud.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_refund")
public class PayRefundDO extends BaseEntity {

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
    /** 退款原因 */
    private String reason;
    /** 第三方退款单号 */
    private String transactionId;
    /** 申请人用户 ID */
    private Long applicantUserId;
    /** 申请人姓名 */
    private String applicantName;
    /** 审核人用户 ID */
    private Long auditorUserId;
    /** 审核人姓名 */
    private String auditorName;
    /** 审核备注 */
    private String auditRemark;
    /** 退款完成时间 */
    private LocalDateTime refundTime;
}
