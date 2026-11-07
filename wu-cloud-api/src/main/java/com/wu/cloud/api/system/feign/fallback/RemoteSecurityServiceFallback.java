package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.dto.SecurityEventDTO;
import com.wu.cloud.api.system.feign.RemoteSecurityService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 远程安全风控服务 Feign 降级工厂
 * <p>风控上报属于旁路动作：降级时仅记录告警，不影响业务请求。</p>
 */
@Slf4j
@Component
public class RemoteSecurityServiceFallback implements FallbackFactory<RemoteSecurityService> {

    @Override
    public RemoteSecurityService create(Throwable cause) {
        log.warn("[Feign降级] wu-service-system 风控服务调用失败: {}", cause.getMessage());
        return new RemoteSecurityService() {
            @Override
            public R<Map<String, Object>> reportEvent(SecurityEventDTO dto) {
                return R.fail("风控服务暂不可用");
            }

            @Override
            public R<Boolean> isBlocked(String principal) {
                // 封禁查询失败按放行处理，优先保证站点可用
                return R.ok(false);
            }
        };
    }
}
