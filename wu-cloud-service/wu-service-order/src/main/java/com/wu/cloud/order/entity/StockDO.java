package com.wu.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品库存
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock")
public class StockDO extends BaseEntity {

    /** 商品编码 */
    private String skuCode;
    /** 商品名称 */
    private String skuName;
    /** 总库存 */
    private Integer totalStock;
    /** 锁定库存 */
    private Integer lockedStock;
    /** 可用库存 */
    private Integer availableStock;
}
