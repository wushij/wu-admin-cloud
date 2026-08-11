package com.wu.cloud.pay.service;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wu.cloud.pay.entity.PayConfigDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付（APIv3 Native 支付，基于 wechat-pay-java 真实接入）
 */
@Slf4j
@Service
public class WechatPayService extends AbstractPayService {

    @Resource
    private PayConfigService payConfigService;

    /**
     * 从支付配置表加载启用的微信配置并构造 APIv3 Config
     * <p>注意：wechat-pay-java 的商户私钥与商户证书序列号是分开的配置项，
     * {@code certPath} 存放商户私钥（PEM），{@code certSerialNo} 单独存放证书序列号，
     * 两者不可混用。</p>
     */
    private Config buildConfig(PayConfigDO config) {
        String mchId = config.getMchId();
        String apiV3Key = config.getApiKey();          // APIv3 密钥
        String privateKey = config.getCertPath();      // 商户私钥（PEM 文本）
        String certSerialNo = config.getCertSerialNo(); // 商户证书序列号（16 位十六进制）

        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(privateKey)
                || !StringUtils.hasText(certSerialNo)) {
            throw new RuntimeException("微信支付配置不完整，请检查商户号、APIv3密钥、商户私钥、证书序列号");
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(certSerialNo)
                .apiV3Key(apiV3Key)
                .build();
    }

    private PayConfigDO loadConfig() {
        PayConfigDO config = payConfigService.getEnabledConfig("WECHAT");
        if (config == null) {
            throw new RuntimeException("微信支付未启用或配置不存在");
        }
        return config;
    }

    @Override
    public Map<String, String> createTestOrder() {
        String orderNo = "WX" + System.currentTimeMillis();
        return createOrder(orderNo, new BigDecimal("0.01"), "支付测试订单");
    }

    /**
     * 微信 Native 下单
     */
    @Override
    public Map<String, String> createOrder(String orderNo, BigDecimal amount, String subject) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payType", "WECHAT");
        result.put("subject", subject);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        try {
            PayConfigDO config = loadConfig();
            NativePayService service = new NativePayService.Builder().config(buildConfig(config)).build();
            PrepayRequest request = new PrepayRequest();
            request.setAppid(config.getAppId());
            request.setMchid(config.getMchId());
            request.setDescription(subject != null ? subject : "订单");
            request.setOutTradeNo(orderNo);
            if (StringUtils.hasText(config.getNotifyUrl())) {
                request.setNotifyUrl(config.getNotifyUrl());
            }
            Amount payAmount = new Amount();
            // 微信金额单位：分；setTotal 接收 Integer，需防 int 溢出（约 2147 万元上限）
            long totalFen = amount == null ? 0L : amount.multiply(BigDecimal.valueOf(100)).longValueExact();
            if (totalFen > Integer.MAX_VALUE) {
                throw new RuntimeException("支付金额超出微信单笔上限");
            }
            payAmount.setTotal((int) totalFen);
            payAmount.setCurrency("CNY");
            request.setAmount(payAmount);
            PrepayResponse response = service.prepay(request);
            String codeUrl = response.getCodeUrl();
            result.put("qrContent", codeUrl);
            result.put("prepayId", codeUrl);
            log.info("[WechatPay] Native 下单成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("[WechatPay] Native 下单失败 orderNo={}", orderNo, e);
            throw new RuntimeException("微信支付下单失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 微信查单
     */
    @Override
    public Map<String, String> queryOrder(String orderNo) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        try {
            PayConfigDO config = loadConfig();
            NativePayService service = new NativePayService.Builder().config(buildConfig(config)).build();
            QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
            request.setMchid(config.getMchId());
            request.setOutTradeNo(orderNo);
            Transaction transaction = service.queryOrderByOutTradeNo(request);
            String tradeState = transaction.getTradeState() != null
                    ? transaction.getTradeState().name() : "UNKNOWN";
            result.put("tradeState", tradeState);
            result.put("tradeStateDesc", transaction.getTradeStateDesc());
            if (transaction.getTransactionId() != null) {
                result.put("transactionId", transaction.getTransactionId());
            }
            log.info("[WechatPay] 查单 orderNo={} state={}", orderNo, tradeState);
        } catch (Exception e) {
            log.warn("[WechatPay] 查单失败 orderNo={}: {}", orderNo, e.getMessage());
            result.put("tradeState", "NOTPAY");
            result.put("tradeStateDesc", "查单异常，按未支付处理: " + e.getMessage());
        }
        return result;
    }

    /**
     * 微信关单
     */
    @Override
    public void closeOrder(String orderNo) {
        try {
            PayConfigDO config = loadConfig();
            NativePayService service = new NativePayService.Builder().config(buildConfig(config)).build();
            com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest closeReq =
                    new com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest();
            closeReq.setMchid(config.getMchId());
            closeReq.setOutTradeNo(orderNo);
            service.closeOrder(closeReq);
            log.info("[WechatPay] 关闭订单: {}", orderNo);
        } catch (Exception e) {
            log.warn("[WechatPay] 关单失败 orderNo={}: {}", orderNo, e.getMessage());
        }
    }

    /**
     * 微信退款
     */
    @Override
    public Map<String, String> refund(String orderNo, String refundNo, BigDecimal amount) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("refundNo", refundNo);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        try {
            PayConfigDO config = loadConfig();
            RefundService refundService = new RefundService.Builder().config(buildConfig(config)).build();
            com.wechat.pay.java.service.refund.model.CreateRequest request =
                    new com.wechat.pay.java.service.refund.model.CreateRequest();
            request.setOutTradeNo(orderNo);
            request.setOutRefundNo(refundNo);
            if (StringUtils.hasText(config.getRefundNotifyUrl())) {
                request.setNotifyUrl(config.getRefundNotifyUrl());
            }
            com.wechat.pay.java.service.refund.model.AmountReq refundAmount =
                    new com.wechat.pay.java.service.refund.model.AmountReq();
            long fen = amount == null ? 0 : amount.multiply(BigDecimal.valueOf(100)).longValue();
            refundAmount.setRefund(fen);
            refundAmount.setTotal(fen);
            refundAmount.setCurrency("CNY");
            request.setAmount(refundAmount);
            var transaction = refundService.create(request);
            result.put("refundId", transaction.getRefundId());
            result.put("refundStatus", "PROCESSING");
            log.info("[WechatPay] 退款申请成功: refundNo={}", refundNo);
        } catch (Exception e) {
            log.error("[WechatPay] 退款申请失败 refundNo={}", refundNo, e);
            result.put("refundStatus", "FAILED");
            throw new RuntimeException("微信退款失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getPayType() {
        return "WECHAT";
    }

    @Override
    public String getPayTypeName() {
        return "微信支付";
    }
}
