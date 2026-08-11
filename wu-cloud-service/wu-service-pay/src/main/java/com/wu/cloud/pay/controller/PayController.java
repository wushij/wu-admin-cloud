package com.wu.cloud.pay.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.service.PayNotifyService;
import com.wu.cloud.pay.service.PayOrderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Resource
    private PayOrderService payOrderService;
    @Resource
    private PayNotifyService payNotifyService;

    /**
     * 创建支付订单。
     *
     * <p>金额由调用方显式传入并透传给第三方渠道下单，不再使用固定 0.01 元的测试下单。
     * {@code orderNo} 用于关联上游业务订单，缺省时由支付服务生成。</p>
     */
    @PostMapping("/create")
    public R<Map<String, String>> create(@RequestParam String payType,
                                          @RequestParam BigDecimal amount,
                                          @RequestParam(required = false) String orderNo,
                                          @RequestParam(required = false) String subject) {
        return R.ok(payOrderService.createOrder(payType, amount, orderNo, subject));
    }

    /**
     * 微信支付回调（真实 V3 验签）
     * <p>微信以 JSON body + Wechatpay-* 请求头回调，返回 SUCCESS / FAIL。</p>
     */
    @PostMapping("/notify/wechat")
    public String wechatNotify(HttpServletRequest request, @RequestBody String body) {
        return payNotifyService.handleWechatNotify(
                request.getHeader("Wechatpay-Serial"),
                request.getHeader("Wechatpay-Nonce"),
                request.getHeader("Wechatpay-Timestamp"),
                request.getHeader("Wechatpay-Signature"),
                body);
    }

    /**
     * 支付宝回调（真实 RSA2 验签）
     * <p>支付宝以 form 参数回调，返回 success / failure。</p>
     */
    @PostMapping("/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return payNotifyService.handleAlipayNotify(params);
    }

    /**
     * 查询支付订单状态
     */
    @GetMapping("/status")
    public R<PayOrderDO> status(@RequestParam String orderNo) {
        return R.ok(payOrderService.syncOrderStatus(orderNo));
    }

    /**
     * B 队修复：GET /api/pay/order/{orderNo}。
     * 兼容前端 wu-admin-web config/usePaymentTest.pollPayOrderStatus 的
     * getPayOrderStatus(orderNo) 调用（见 config/index.ts getPayOrderStatus）。
     */
    @GetMapping("/order/{orderNo}")
    public R<PayOrderDO> getOrderStatus(@PathVariable String orderNo) {
        return R.ok(payOrderService.syncOrderStatus(orderNo));
    }

    @GetMapping("/list")
    public R<List<PayOrderDO>> list(@RequestParam String orderNo) {
        return R.ok(payOrderService.listByOrderNo(orderNo));
    }

    @GetMapping("/types")
    public R<List<String>> supportedTypes() {
        return R.ok(List.of("ALIPAY", "WECHAT", "BALANCE"));
    }

    /**
     * 支付记录对账：统计指定日期范围内的应支付 / 已支付 / 差额
     */
    @GetMapping("/reconcile")
    public R<Map<String, Object>> reconcile(@RequestParam String startDate,
                                            @RequestParam String endDate) {
        return R.ok(payOrderService.reconcile(startDate, endDate));
    }
}
