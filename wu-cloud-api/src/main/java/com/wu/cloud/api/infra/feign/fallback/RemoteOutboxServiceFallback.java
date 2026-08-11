package com.wu.cloud.api.infra.feign.fallback;

import com.wu.cloud.api.infra.dto.OutboxDTO;
import com.wu.cloud.api.infra.feign.RemoteOutboxService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 本地消息表 Feign 降级（P0-3 + P1-4 修复）
 */
@Slf4j
@Component
public class RemoteOutboxServiceFallback implements FallbackFactory<RemoteOutboxService> {

    @Override
    public RemoteOutboxService create(Throwable cause) {
        log.error("[Feign降级] wu-service-infra 本地消息表服务调用失败", cause);
        return new RemoteOutboxService() {
            @Override
            public R<Long> save(OutboxDTO dto) {
                return R.fail("本地消息表服务暂不可用");
            }
        };
    }
}
