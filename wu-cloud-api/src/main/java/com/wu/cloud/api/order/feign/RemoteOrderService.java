package com.wu.cloud.api.order.feign;

import com.wu.cloud.api.order.dto.TradeOrderDTO;
import com.wu.cloud.api.order.feign.fallback.RemoteOrderServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程订单服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteOrderService", value = "wu-service-order", path = "/inner/order", fallbackFactory = RemoteOrderServiceFallback.class)
public interface RemoteOrderService {

    @GetMapping("/{id}")
    R<TradeOrderDTO> getOrderById(@PathVariable("id") Long id);

    /**
     * 支付成功回调：按业务订单号将订单置为已支付（幂等）。
     */
    @PostMapping("/notify-paid")
    R<Boolean> markPaidByOrderNo(@RequestParam("orderNo") String orderNo,
                                 @RequestParam(value = "payType", required = false) String payType);
}
