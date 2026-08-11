package com.wu.cloud.order.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageDTO extends PageQuery {
    private String orderNo;
    private Long userId;
    private String status;
}
