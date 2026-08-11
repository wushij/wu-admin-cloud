package com.wu.cloud.pay.service;

import com.alipay.api.internal.util.AlipaySignature;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.pay.entity.PayConfigDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 支付回调处理：真实验签（微信 V3 / 支付宝 RSA2）+ 状态流转
 * <p>替代原先"signature 非空即通过"的模拟逻辑。</p>
 */
@Slf4j
@Service
public class PayNotifyService {

    @Resource
    private PayConfigService payConfigService;
    @Resource
    private PayOrderService payOrderService;

    /**
     * 微信支付回调：V3 签名验证
     *
     * @param serial    Wechatpay-Serial 请求头
     * @param nonce     Wechatpay-Nonce 请求头
     * @param timestamp Wechatpay-Timestamp 请求头
     * @param signature Wechatpay-Signature 请求头
     * @param body      回调请求体（JSON）
     */
    @SuppressWarnings("deprecation")
    public String handleWechatNotify(String serial, String nonce, String timestamp, String signature, String body) {
        if (!StringUtils.hasText(serial) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(timestamp) || !StringUtils.hasText(signature) || !StringUtils.hasText(body)) {
            log.warn("[WechatPay] 回调参数不完整，验签失败");
            return "FAIL";
        }
        try {
            PayConfigDO config = payConfigService.getEnabledConfig("WECHAT");
            AutoCertificateNotificationConfig notificationConfig = buildWechatNotificationConfig(config);
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(body)
                    .build();
            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            if (Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
                String orderNo = transaction.getOutTradeNo();
                String transactionId = transaction.getTransactionId();
                // 回调验签通过后标记已支付
                payOrderService.handleNotifyInternal("WECHAT", orderNo, transactionId);
                log.info("[WechatPay] 回调成功: orderNo={}, transactionId={}", orderNo, transactionId);
            }
            return "SUCCESS";
        } catch (Exception e) {
            log.error("[WechatPay] 回调处理失败", e);
            return "FAIL";
        }
    }

    /**
     * 支付宝回调：RSA2 验签
     */
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            PayConfigDO config = payConfigService.getEnabledConfig("ALIPAY");
            String publicKey = config.getCertPath();
            if (!StringUtils.hasText(publicKey)) {
                log.error("[Alipay] 支付宝公钥未配置");
                return "failure";
            }
            // 必须移除 sign 与 sign_type 后再验签
            if (!AlipaySignature.rsaCheckV1(params, publicKey, "UTF-8", "RSA2")) {
                log.error("[Alipay] 回调验签失败: orderNo={}", params.get("out_trade_no"));
                return "failure";
            }
            String tradeStatus = params.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                payOrderService.handleNotifyInternal("ALIPAY", params.get("out_trade_no"), params.get("trade_no"));
                log.info("[Alipay] 回调成功: orderNo={}", params.get("out_trade_no"));
            }
            return "success";
        } catch (Exception e) {
            log.error("[Alipay] 回调处理失败", e);
            return "failure";
        }
    }

    @SuppressWarnings("deprecation")
    private AutoCertificateNotificationConfig buildWechatNotificationConfig(PayConfigDO config) {
        String mchId = config.getMchId();
        String apiV3Key = config.getApiKey();
        String privateKey = config.getCertPath();
        String certSerialNo = config.getCertSerialNo();
        if (!StringUtils.hasText(mchId) || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(privateKey)
                || !StringUtils.hasText(certSerialNo)) {
            throw new BusinessException("微信支付配置不完整（商户号/APIv3密钥/商户私钥/证书序列号）");
        }
        return new AutoCertificateNotificationConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(certSerialNo)
                .apiV3Key(apiV3Key)
                .build();
    }
}
