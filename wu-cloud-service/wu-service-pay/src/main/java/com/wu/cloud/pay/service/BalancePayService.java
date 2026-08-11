package com.wu.cloud.pay.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 余额支付（内部直接标记支付成功，用于演示/内部结算）
 */
@Service
public class BalancePayService extends AbstractPayService {

    @Override
    public Map<String, String> createTestOrder() {
        Map<String, String> result = new HashMap<>();
        String orderNo = "BAL" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        result.put("orderNo", orderNo);
        result.put("payType", "BALANCE");
        result.put("qrContent", "balance://pay/" + orderNo);
        return result;
    }

    /**
     * 余额下单（内部支付，直接返回成功）
     */
    @Override
    public Map<String, String> createOrder(String orderNo, BigDecimal amount, String subject) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payType", "BALANCE");
        result.put("subject", subject);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        result.put("qrContent", "balance://pay/" + orderNo);
        // 余额支付即时完成
        result.put("tradeState", "SUCCESS");
        return result;
    }

    /**
     * 余额查单（内部直接返回成功状态）
     */
    @Override
    public Map<String, String> queryOrder(String orderNo) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        // 余额支付无第三方异步状态，直接返回成功
        result.put("tradeState", "SUCCESS");
        result.put("tradeStateDesc", "余额支付成功");
        return result;
    }

    /**
     * 余额关单（内部无需关闭）
     */
    @Override
    public void closeOrder(String orderNo) {
        log.info("[BalancePay] 关闭订单: {}", orderNo);
    }

    /**
     * 余额退款（内部直接成功）
     */
    @Override
    public Map<String, String> refund(String orderNo, String refundNo, BigDecimal amount) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("refundNo", refundNo);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        // 余额退款即时完成
        result.put("refundId", "bal_refund_" + refundNo);
        result.put("refundStatus", "SUCCESS");
        return result;
    }

    @Override
    public String getPayType() {
        return "BALANCE";
    }

    @Override
    public String getPayTypeName() {
        return "余额支付";
    }
}
