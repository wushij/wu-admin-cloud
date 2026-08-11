package com.wu.cloud.api.infra.feign.fallback;

import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 在线用户 Feign 降级
 */
@Slf4j
@Component
public class RemoteOnlineUserServiceFallback implements FallbackFactory<RemoteOnlineUserService> {

    @Override
    public RemoteOnlineUserService create(Throwable cause) {
        log.error("[Feign降级] wu-service-infra 在线用户服务调用失败", cause);
        return new RemoteOnlineUserService() {
            @Override
            public R<Boolean> recordLoginSession(RecordLoginSessionDTO dto) {
                return R.ok(false);
            }

            @Override
            public R<Boolean> touchLastAccess(Long userId) {
                return R.ok(false);
            }

            @Override
            public R<Integer> countOnlineUsers() {
                return R.ok(0);
            }
        };
    }
}
