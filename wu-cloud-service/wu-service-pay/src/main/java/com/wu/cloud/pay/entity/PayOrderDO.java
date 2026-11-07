package com.wu.cloud.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单（落库，替代旧版内存 PayOrderStore）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_order")
public class PayOrderDO extends BaseEntity {

    /** 业务订单号 */
    private String orderNo;
    /** 支付类型: ALIPAY / WECHAT / BALANCE */
    private String payType;
    /** 状态: PENDING / PAID / CLOSED */
    private String status;
    /** 第三方交易号 */
    private String transactionId;
    /** 金额 */
    private BigDecimal amount;
    private LocalDateTime paidTime;
}
