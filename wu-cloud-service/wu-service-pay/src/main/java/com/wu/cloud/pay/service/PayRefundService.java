package com.wu.cloud.pay.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.pay.entity.PayRefundDO;

/**
 * 退款单 Service
 */
public interface PayRefundService {

    /**
     * 分页查询退款单
     */
    PageResult<PayRefundDO> page(PageParam pageParam, String refundNo, String orderNo, String status);

    /**
     * 退款单详情
     */
    PayRefundDO detail(Long id);

    /**
     * 申请退款：关联原支付订单，创建 PENDING 退款单
     */
    Long apply(Long orderId, String reason);

    /**
     * 按业务订单号申请退款（内部 RPC 使用）：创建退款单并联动第三方渠道执行
     *
     * @param orderNo 业务订单号
     * @param amount  退款金额
     * @param reason  退款原因
     * @return 退款单（已包含执行后的状态与第三方单号）
     */
    PayRefundDO applyByOrderNo(String orderNo, java.math.BigDecimal amount, String reason);

    /**
     * 审核退款：approved → APPROVED 后自动 execute；rejected → REJECTED
     */
    void audit(Long id, Boolean approved, String remark);

    /**
     * 执行退款：调用第三方退款（模拟），PROCESSING → SUCCESS
     */
    void execute(Long id);

    /**
     * 第三方退款回调：更新退款状态
     */
    void handleRefundNotify(String refundNo, String transactionId, boolean success);
}
