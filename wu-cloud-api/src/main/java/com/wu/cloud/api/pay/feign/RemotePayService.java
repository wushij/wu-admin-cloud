package com.wu.cloud.api.pay.feign;

import com.wu.cloud.api.pay.dto.PayOrderDTO;
import com.wu.cloud.api.pay.dto.PayRefundDTO;
import com.wu.cloud.api.pay.feign.fallback.RemotePayServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 远程支付服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remotePayService", value = "wu-service-pay", path = "/inner/pay", fallbackFactory = RemotePayServiceFallback.class)
public interface RemotePayService {

    @GetMapping("/{id}")
    R<PayOrderDTO> getPayOrderById(@PathVariable("id") Long id);

    /**
     * 发起退款：创建退款单并调用第三方渠道退款
     *
     * @param orderNo 业务订单号
     * @param amount  退款金额
     * @param reason  退款原因
     */
    @PostMapping("/refund")
    R<PayRefundDTO> applyRefund(@RequestParam("orderNo") String orderNo,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "reason", required = false) String reason);
}
