package com.wu.cloud.pay.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付服务接口（替代单体 framework.pay.PayService）
 */
public interface PayService {

    /**
     * 创建一笔<b>固定 0.01 元</b>的测试支付订单，仅用于渠道连通性自检。
     *
     * @deprecated 该方法金额写死且自行生成订单号，<b>严禁用于真实交易链路</b>
     *             （曾导致下单金额与业务订单金额不一致的资损问题）。
     *             真实下单请使用 {@link #createOrder(String, BigDecimal, String)}。
     */
    @Deprecated
    Map<String, String> createTestOrder();

    /**
     * 统一下单，返回二维码内容 / 跳转链接等下单参数
     */
    Map<String, String> createOrder(String orderNo, BigDecimal amount, String subject);

    /**
     * 查询第三方订单状态，返回 trade_state / 状态等
     */
    Map<String, String> queryOrder(String orderNo);

    /**
     * 关闭第三方订单
     */
    void closeOrder(String orderNo);

    /**
     * 发起退款，返回退款单号等
     */
    Map<String, String> refund(String orderNo, String refundNo, BigDecimal amount);

    String getPayType();

    String getPayTypeName();
}
