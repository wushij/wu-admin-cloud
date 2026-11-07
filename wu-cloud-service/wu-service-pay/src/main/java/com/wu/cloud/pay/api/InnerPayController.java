package com.wu.cloud.pay.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.pay.dto.PayOrderDTO;
import com.wu.cloud.api.pay.dto.PayRefundDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.entity.PayRefundDO;
import com.wu.cloud.pay.mapper.PayOrderMapper;
import com.wu.cloud.pay.service.PayRefundService;
import com.wu.cloud.pay.service.PayServiceFactory;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付服务内部 RPC 端点。
 *
 * <p>契约对齐：返回值统一使用 DTO（与 wu-cloud-api 中的
 * {@code RemotePayService} 声明一致），避免 Feign 反序列化
 * 时 DO/DTO 字段不一致导致的静默失败。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/pay")
public class InnerPayController {

    @Resource
    private PayOrderMapper payOrderMapper;
    @Resource
    private PayRefundService payRefundService;
    @Resource
    private PayServiceFactory payServiceFactory;

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<PayOrderDTO> getPayOrderById(@PathVariable("id") Long id) {
        PayOrderDO order = payOrderMapper.selectById(id);
        if (order == null) {
            return R.ok(null);
        }
        return R.ok(toDTO(order));
    }

    /**
     * 发起退款：按业务订单号创建退款单并联动第三方渠道执行真实退款
     * <p>由 order 服务的 {@code TradeRefundServiceImpl.audit()} 联动调用。</p>
     */
    @Operation(summary = "新增")
    @PostMapping("/refund")
    public R<PayRefundDTO> applyRefund(@RequestParam("orderNo") String orderNo,
                                       @RequestParam("amount") BigDecimal amount,
                                       @RequestParam(value = "reason", required = false) String reason) {
        PayRefundDO refund = payRefundService.applyByOrderNo(orderNo, amount, reason);
        return R.ok(toRefundDTO(refund));
    }

    /**
     * 支付渠道连通性测试：用 pay_config 真实配置向渠道发起 0.01 元测试预下单，
     * 返回订单号 / 支付链接 / 二维码，验证配置可用性（不落业务订单表）。
     */
    @Operation(summary = "连通性测试")
    @PostMapping("/test-channel")
    @SuppressWarnings("deprecation")
    public R<Map<String, String>> testChannel(@RequestParam("payType") String payType) {
        try {
            return R.ok(payServiceFactory.getService(payType).createTestOrder());
        } catch (Exception e) {
            return R.fail("渠道测试失败：" + e.getMessage());
        }
    }

    /**
     * DO → DTO 映射，仅暴露跨服务调用所需的字段，避免泄露内部细节
     */
    private PayOrderDTO toDTO(PayOrderDO order) {
        PayOrderDTO dto = new PayOrderDTO();
        dto.setId(order.getId());
        dto.setPayNo(order.getOrderNo());
        dto.setMerchantOrderNo(order.getOrderNo());
        dto.setChannelCode(order.getPayType());
        dto.setAmount(order.getAmount());
        dto.setStatus(order.getStatus());
        dto.setCreateTime(order.getCreateTime());
        return dto;
    }

    private PayRefundDTO toRefundDTO(PayRefundDO refund) {
        PayRefundDTO dto = new PayRefundDTO();
        dto.setId(refund.getId());
        dto.setRefundNo(refund.getRefundNo());
        dto.setOrderNo(refund.getOrderNo());
        dto.setPayType(refund.getPayType());
        dto.setAmount(refund.getAmount());
        dto.setStatus(refund.getStatus());
        dto.setTransactionId(refund.getTransactionId());
        dto.setCreateTime(refund.getCreateTime());
        return dto;
    }
}
