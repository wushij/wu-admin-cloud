package com.wu.cloud.api.pay.feign.fallback;

import com.wu.cloud.api.pay.dto.PayOrderDTO;
import com.wu.cloud.api.pay.dto.PayRefundDTO;
import com.wu.cloud.api.pay.feign.RemotePayService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 远程支付服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemotePayServiceFallback implements FallbackFactory<RemotePayService> {

    @Override
    public RemotePayService create(Throwable cause) {
        log.error("[Feign降级] wu-service-pay 支付服务调用失败", cause);
        return new RemotePayService() {
            @Override
            public R<PayOrderDTO> getPayOrderById(Long id) {
                return R.fail("支付服务暂不可用");
            }

            @Override
            public R<PayRefundDTO> applyRefund(String orderNo, BigDecimal amount, String reason) {
                return R.fail("支付服务暂不可用，退款失败");
            }

            @Override
            public R<Map<String, String>> testChannel(String payType) {
                return R.fail("支付服务暂不可用，渠道测试失败");
            }
        };
    }
}
