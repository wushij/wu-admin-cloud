package com.wu.cloud.order.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.order.entity.TradeRefundDO;

/**
 * 退款单 Service
 */
public interface TradeRefundService {

    /** 分页查询 */
    PageResult<TradeRefundDO> page(PageParam pageParam, String orderNo, String status);

    /** 退款单详情 */
    TradeRefundDO detail(Long id);

    /** 申请退款：关联订单创建退款单（PENDING） */
    Long apply(Long orderId, String reason);

    /** 审核退款：通过则调用支付服务发起退款，成功后订单状态置为 REFUNDED */
    void audit(Long id, Boolean approved, String remark);
}
