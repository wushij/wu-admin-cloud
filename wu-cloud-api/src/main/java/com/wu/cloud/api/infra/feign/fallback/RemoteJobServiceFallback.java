package com.wu.cloud.api.infra.feign.fallback;

import com.wu.cloud.api.infra.feign.RemoteJobService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class RemoteJobServiceFallback implements FallbackFactory<RemoteJobService> {

    @Override
    public RemoteJobService create(Throwable cause) {
        log.error("[Feign降级] wu-service-infra 任务统计调用失败", cause);
        return () -> R.ok(Collections.emptyMap());
    }
}
