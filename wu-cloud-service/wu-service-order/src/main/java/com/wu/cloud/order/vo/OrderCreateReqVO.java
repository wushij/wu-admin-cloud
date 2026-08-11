package com.wu.cloud.order.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建请求 VO
 */
@Data
public class OrderCreateReqVO implements Serializable {

    private BigDecimal totalAmount;
    private String remark;
}
