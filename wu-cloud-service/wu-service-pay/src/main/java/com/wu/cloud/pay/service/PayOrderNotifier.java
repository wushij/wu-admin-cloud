package com.wu.cloud.pay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wu.cloud.api.infra.dto.OutboxDTO;
import com.wu.cloud.api.infra.feign.RemoteOutboxService;
import com.wu.cloud.api.order.feign.RemoteOrderService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付成功 → 订单服务的最终一致性通知器（P1-2 修复）。
 *
 * <p><b>背景</b>：此前 pay 服务支付成功后<b>完全没有</b>通知 order 服务，
 * 且 pay 是唯一未开启 {@code @EnableFeignClients} 的服务；
 * infra 侧虽已建好本地消息表（infra_outbox）与补偿任务 OutboxCompensateJob，
 * 但全局没有任何调用方，等于「基建建好了却没接线」。
 * 结果是用户付款成功、pay_order 为 PAID，而 trade_order 永远停留在 PENDING。</p>
 *
 * <p><b>策略</b>：同步优先 + 异步兜底</p>
 * <ol>
 *   <li>先尝试同步 Feign 调用 order 的 {@code /inner/order/notify-paid}（幂等），
 *       成功则链路最短、用户体验最好；</li>
 *   <li>同步失败（order 宕机/网络抖动/降级）时，写入 infra_outbox，
 *       由 OutboxCompensateJob 以指数退避重试，最多 5 次后标记 DEAD 供人工介入；</li>
 *   <li>若连 outbox 也写入失败，打 ERROR 日志告警——此时需要对账任务兜底，
 *       绝不能吞掉异常导致资金状态静默不一致。</li>
 * </ol>
 *
 * <p><b>不影响主流程</b>：本类所有异常都被捕获，不会让第三方回调收到失败响应
 * 而触发渠道侧无意义的重复回调（本地入账已完成，重复回调只会被幂等拦截）。</p>
 */
@Slf4j
@Component
public class PayOrderNotifier {

    private static final String TARGET_SERVICE = "wu-service-order";
    private static final String TARGET_API = "/inner/order/notify-paid";

    @Resource
    private RemoteOrderService remoteOrderService;
    @Resource
    private RemoteOutboxService remoteOutboxService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 通知订单服务「该业务订单已支付」。
     *
     * @param orderNo 业务订单号
     * @param payType 支付方式
     */
    public void notifyOrderPaid(String orderNo, String payType) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        // 1) 同步优先
        try {
            R<Boolean> resp = remoteOrderService.markPaidByOrderNo(orderNo, payType);
            if (resp != null && resp.getCode() == R.SUCCESS) {
                log.info("[Pay->Order] 同步通知成功: orderNo={}, matched={}", orderNo, resp.getData());
                return;
            }
            log.warn("[Pay->Order] 同步通知未成功，转入本地消息表补偿: orderNo={}, resp={}",
                    orderNo, resp == null ? "null" : resp.getMsg());
        } catch (Exception e) {
            log.warn("[Pay->Order] 同步通知异常，转入本地消息表补偿: orderNo={}, err={}", orderNo, e.getMessage());
        }

        // 2) 异步兜底
        saveOutbox(orderNo, payType);
    }

    private void saveOutbox(String orderNo, String payType) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("orderNo", orderNo);
            if (payType != null) {
                payload.put("payType", payType);
            }

            OutboxDTO dto = new OutboxDTO();
            // bizKey 具备业务唯一性，便于排查与去重
            dto.setBizKey("pay:order:paid:" + orderNo);
            dto.setTargetService(TARGET_SERVICE);
            dto.setTargetApi(TARGET_API);
            dto.setPayload(objectMapper.writeValueAsString(payload));

            remoteOutboxService.save(dto);
            log.info("[Pay->Order] 已写入本地消息表待补偿: orderNo={}", orderNo);
        } catch (Exception e) {
            // 兜底失败属于严重问题：此时只能依赖对账任务发现，必须告警
            log.error("[Pay->Order] 写入本地消息表失败，订单状态可能不一致，请人工核查! orderNo={}", orderNo, e);
        }
    }
}
