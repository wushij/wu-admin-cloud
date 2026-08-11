package com.wu.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order")
public class TradeOrderDO extends BaseEntity {

    /** 订单编号 */
    private String orderNo;
    /** 用户 ID */
    private Long userId;
    /** 商品标题 */
    private String subject;
    /** 总金额 */
    private BigDecimal totalAmount;
    /** 实付金额 */
    private BigDecimal payAmount;
    /** 支付方式: ALIPAY/WECHAT/BALANCE */
    private String payType;
    /** 状态: PENDING/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED */
    private String status;
    /** 支付时间 */
    private java.time.LocalDateTime payTime;
    /** 完成时间 */
    private java.time.LocalDateTime finishTime;

    /** 订单明细列表（非持久化，仅用于创建订单时携带商品信息） */
    @TableField(exist = false)
    private List<OrderItemDO> items;
}
