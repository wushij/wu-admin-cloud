package com.wu.cloud.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.order.dto.TradeOrderDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.mapper.TradeOrderMapper;
import com.wu.cloud.order.service.TradeOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订单服务内部 RPC 端点。
 * <p>契约对齐：返回值统一使用 {@code TradeOrderDTO}，与 wu-cloud-api 中
 * {@code RemoteOrderService} 声明一致。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/order")
public class InnerOrderController {

    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private TradeOrderService tradeOrderService;

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<TradeOrderDTO> getOrderById(@PathVariable("id") Long id) {
        return R.ok(toDTO(tradeOrderMapper.selectById(id)));
    }

    /**
     * 支付服务支付成功回调：按业务订单号将订单置为已支付（幂等）。
     *
     * <p>pay 服务在第三方回调验签通过后调用，确保 pay_order 与 trade_order 的
     * 「已支付」状态一致。</p>
     *
     * <p><b>同时兼容两种调用方式</b>：</p>
     * <ul>
     *   <li>Feign 直连：{@code ?orderNo=xxx&payType=xxx}（query 参数）；</li>
     *   <li>Outbox 补偿任务：{@code OutboxCompensateJob} 以 JSON body 形式 POST
     *       （{@code {"orderNo":"...","payType":"..."}}）。若只支持 @RequestParam，
     *       补偿请求会因参数缺失返回 400，导致重试到 DEAD 仍无法落地。</li>
     * </ul>
     */
    @Operation(summary = "批量已读")
    @PostMapping("/notify-paid")
    public R<Boolean> notifyPaid(@RequestParam(value = "orderNo", required = false) String orderNo,
                                 @RequestParam(value = "payType", required = false) String payType,
                                 @RequestBody(required = false) Map<String, String> body) {
        String finalOrderNo = orderNo;
        String finalPayType = payType;
        if (body != null) {
            if (finalOrderNo == null) {
                finalOrderNo = body.get("orderNo");
            }
            if (finalPayType == null) {
                finalPayType = body.get("payType");
            }
        }
        if (finalOrderNo == null || finalOrderNo.isBlank()) {
            return R.fail("orderNo 不能为空");
        }
        return R.ok(tradeOrderService.markPaidByOrderNo(finalOrderNo, finalPayType));
    }

    private TradeOrderDTO toDTO(TradeOrderDO order) {
        if (order == null) {
            return null;
        }
        TradeOrderDTO dto = new TradeOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setStatus(order.getStatus());
        dto.setCreateTime(order.getCreateTime());
        return dto;
    }
}
