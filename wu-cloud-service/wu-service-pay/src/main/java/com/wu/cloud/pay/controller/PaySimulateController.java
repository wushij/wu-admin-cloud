package com.wu.cloud.pay.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.service.PayOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 支付仿真端点（<b>仅限非生产环境</b>）。
 *
 * <p>该接口绕过第三方验签直接把订单标记为已支付，属于资金入口，
 * 一旦在生产暴露即可被任意调用者刷单。因此做三层隔离：</p>
 * <ol>
 *   <li>{@code @Profile("!prod")}：生产 Profile 下该 Bean 根本不会注册，接口不存在（404）；</li>
 *   <li>网关 {@code PayApiBlockFilter} 拦截 {@code /api/pay/notify/simulate}，外部不可达；</li>
 *   <li>调用时打印 WARN 日志，便于审计。</li>
 * </ol>
 *
 * <p>从 {@code PayController} 中拆出，避免与真实支付端点混在同一个类里被误放行。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@Profile("!prod")
public class PaySimulateController {

    @Resource
    private PayOrderService payOrderService;

    /**
     * 模拟支付成功回调（本地联调专用）。
     */
    @PostMapping("/notify/simulate")
    public R<Boolean> simulateNotify(@RequestParam String orderNo) {
        log.warn("[Pay] 收到模拟支付请求（非生产环境专用）: orderNo={}", orderNo);
        String txnId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        payOrderService.simulatePaid(orderNo, txnId);
        return R.ok(true);
    }
}
