package com.wu.cloud.pay.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.wu.cloud.pay.entity.PayConfigDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付（当面付 precreate，基于 alipay-sdk-java 真实接入）
 */
@Slf4j
@Service
public class AlipayService extends AbstractPayService {

    @Resource
    private PayConfigService payConfigService;

    private PayConfigDO loadConfig() {
        PayConfigDO config = payConfigService.getEnabledConfig("ALIPAY");
        if (config == null) {
            throw new RuntimeException("支付宝支付未启用或配置不存在");
        }
        return config;
    }

    private AlipayClient buildAlipayClient(PayConfigDO config) {
        String appId = config.getAppId();
        String privateKey = config.getApiKey();
        String publicKey = config.getCertPath();
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey) || !StringUtils.hasText(publicKey)) {
            throw new RuntimeException("支付宝配置不完整，请检查 AppID、应用私钥、支付宝公钥");
        }
        return new DefaultAlipayClient(
                "https://openapi.alipay.com/gateway.do",
                appId,
                privateKey,
                "json",
                "UTF-8",
                publicKey,
                "RSA2");
    }

    @Override
    public Map<String, String> createTestOrder() {
        String orderNo = "ALI" + System.currentTimeMillis();
        return createOrder(orderNo, new BigDecimal("0.01"), "支付测试订单");
    }

    /**
     * 支付宝当面付预下单
     */
    @Override
    public Map<String, String> createOrder(String orderNo, BigDecimal amount, String subject) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payType", "ALIPAY");
        result.put("subject", subject);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        try {
            PayConfigDO config = loadConfig();
            AlipayClient client = buildAlipayClient(config);
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            if (StringUtils.hasText(config.getNotifyUrl())) {
                request.setNotifyUrl(config.getNotifyUrl());
            }
            String total = amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            String bizContent = "{\"out_trade_no\":\"" + orderNo + "\",\"total_amount\":\"" + total
                    + "\",\"subject\":\"" + (subject != null ? subject : "订单") + "\"}";
            request.setBizContent(bizContent);
            AlipayTradePrecreateResponse response = client.execute(request);
            if (response.isSuccess()) {
                String qrCode = response.getQrCode();
                result.put("qrContent", qrCode);
                result.put("outTradeNo", response.getOutTradeNo());
                log.info("[Alipay] 当面付下单成功: orderNo={}", orderNo);
            } else {
                throw new RuntimeException("支付宝下单失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("[Alipay] 当面付下单失败 orderNo={}", orderNo, e);
            throw new RuntimeException("支付宝下单失败: " + e.getErrMsg());
        }
        return result;
    }

    /**
     * 支付宝查单
     */
    @Override
    public Map<String, String> queryOrder(String orderNo) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        try {
            PayConfigDO config = loadConfig();
            AlipayClient client = buildAlipayClient(config);
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                log.warn("[Alipay] 查单失败 orderNo={}: {}", orderNo, response.getSubMsg());
                result.put("tradeStatus", "WAIT_BUYER_PAY");
                result.put("tradeStatusDesc", "查单失败: " + response.getSubMsg());
                return result;
            }
            String tradeStatus = response.getTradeStatus();
            result.put("tradeStatus", tradeStatus);
            result.put("tradeStatusDesc", tradeStatus);
            if (response.getTradeNo() != null) {
                result.put("tradeNo", response.getTradeNo());
            }
            log.info("[Alipay] 查单 orderNo={} status={}", orderNo, tradeStatus);
        } catch (AlipayApiException e) {
            log.warn("[Alipay] 查单异常 orderNo={}: {}", orderNo, e.getErrMsg());
            result.put("tradeStatus", "WAIT_BUYER_PAY");
            result.put("tradeStatusDesc", "查单异常: " + e.getErrMsg());
        }
        return result;
    }

    /**
     * 支付宝关单
     */
    @Override
    public void closeOrder(String orderNo) {
        try {
            PayConfigDO config = loadConfig();
            AlipayClient client = buildAlipayClient(config);
            com.alipay.api.request.AlipayTradeCloseRequest request =
                    new com.alipay.api.request.AlipayTradeCloseRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            client.execute(request);
            log.info("[Alipay] 关闭订单: {}", orderNo);
        } catch (AlipayApiException e) {
            log.warn("[Alipay] 关单失败 orderNo={}: {}", orderNo, e.getErrMsg());
        }
    }

    /**
     * 支付宝退款
     */
    @Override
    public Map<String, String> refund(String orderNo, String refundNo, BigDecimal amount) {
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("refundNo", refundNo);
        result.put("amount", amount != null ? amount.toPlainString() : "0");
        try {
            PayConfigDO config = loadConfig();
            AlipayClient client = buildAlipayClient(config);
            com.alipay.api.request.AlipayTradeRefundRequest request =
                    new com.alipay.api.request.AlipayTradeRefundRequest();
            String total = amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            request.setBizContent("{\"out_trade_no\":\"" + orderNo
                    + "\",\"refund_amount\":\"" + total + "\",\"out_request_no\":\"" + refundNo + "\"}");
            com.alipay.api.response.AlipayTradeRefundResponse response = client.execute(request);
            if (response.isSuccess()) {
                result.put("tradeNo", response.getTradeNo());
                result.put("refundStatus", "SUCCESS");
                log.info("[Alipay] 退款成功: refundNo={}", refundNo);
            } else {
                result.put("refundStatus", "FAILED");
                throw new RuntimeException("支付宝退款失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("[Alipay] 退款失败 refundNo={}", refundNo, e);
            result.put("refundStatus", "FAILED");
            throw new RuntimeException("支付宝退款失败: " + e.getErrMsg());
        }
        return result;
    }

    @Override
    public String getPayType() {
        return "ALIPAY";
    }

    @Override
    public String getPayTypeName() {
        return "支付宝";
    }
}
