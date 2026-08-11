package com.wu.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 交易订单明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order_item")
public class OrderItemDO extends BaseEntity {

    /** 订单 ID */
    private Long orderId;
    /** 商品编码 */
    private String skuCode;
    /** 商品名称 */
    private String skuName;
    /** 数量 */
    private Integer quantity;
    /** 单价 */
    private BigDecimal price;
}
