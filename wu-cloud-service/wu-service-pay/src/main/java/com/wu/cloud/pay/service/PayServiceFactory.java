package com.wu.cloud.pay.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付服务工厂（替代旧版 framework.pay.PayServiceFactory）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceFactory {

    private final List<PayService> payServices;

    private final Map<String, PayService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (PayService service : payServices) {
            serviceMap.put(service.getPayType(), service);
            log.info("注册支付服务: {} - {}", service.getPayType(), service.getPayTypeName());
        }
    }

    public PayService getService(String payType) {
        String normalized = payType == null ? "" : payType.trim().toUpperCase(java.util.Locale.ROOT);
        PayService service = serviceMap.get(normalized);
        if (service == null) {
            throw new RuntimeException("不支持的支付类型: " + payType);
        }
        return service;
    }

    public boolean isSupported(String payType) {
        if (payType == null) {
            return false;
        }
        return serviceMap.containsKey(payType.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
