package com.wu.cloud.api.order.feign.fallback;

import com.wu.cloud.api.order.dto.TradeOrderDTO;
import com.wu.cloud.api.order.feign.RemoteOrderService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 远程订单服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteOrderServiceFallback implements FallbackFactory<RemoteOrderService> {

    @Override
    public RemoteOrderService create(Throwable cause) {
        log.error("[Feign降级] wu-service-order 订单服务调用失败", cause);
        return new RemoteOrderService() {
            @Override
            public R<TradeOrderDTO> getOrderById(Long id) {
                return R.fail("订单服务暂不可用");
            }

            @Override
            public R<Boolean> markPaidByOrderNo(String orderNo, String payType) {
                log.error("[Feign降级] 订单服务支付回调失败 orderNo={}", orderNo);
                return R.fail("订单服务暂不可用");
            }
        };
    }
}
