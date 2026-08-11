package com.wu.cloud.api.infra.feign;

import com.wu.cloud.api.infra.dto.OutboxDTO;
import com.wu.cloud.api.infra.feign.fallback.RemoteOutboxServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 本地消息表 Feign 契约（P1-4 修复）
 * <p>
 * 业务服务注入此接口，在事务提交后调用 {@code save()} 写入补偿消息。
 * infra 会通过定时任务轮询并调用目标服务 API，保证最终一致性。
 * </p>
 */
@FeignClient(name = "wu-service-infra", path = "/inner/outbox",
        fallbackFactory = RemoteOutboxServiceFallback.class)
public interface RemoteOutboxService {

    @PostMapping("/save")
    R<Long> save(@RequestBody OutboxDTO dto);
}
