package com.wu.cloud.pay.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayOrderPageDTO extends PageQuery {
    private String payNo;
    private String merchantOrderNo;
    private String status;
}
