package com.wu.cloud.api.message.feign.fallback;

import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.api.message.feign.RemoteMessageService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 远程消息服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteMessageServiceFallback implements FallbackFactory<RemoteMessageService> {

    @Override
    public RemoteMessageService create(Throwable cause) {
        log.error("[Feign降级] wu-service-message 消息服务调用失败", cause);
        return new RemoteMessageService() {
            @Override
            public R<SysAnnounceDTO> getAnnounceById(Long id) {
                return R.fail("消息服务暂不可用");
            }

            @Override
            public R<Long> getChatUnreadCount(Long userId) {
                return R.ok(0L);
            }
        };
    }
}
