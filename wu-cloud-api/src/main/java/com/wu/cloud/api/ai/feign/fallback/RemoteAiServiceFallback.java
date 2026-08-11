package com.wu.cloud.api.ai.feign.fallback;

import com.wu.cloud.api.ai.dto.AiChatLogDTO;
import com.wu.cloud.api.ai.feign.RemoteAiService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 远程 AI 服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteAiServiceFallback implements FallbackFactory<RemoteAiService> {

    @Override
    public RemoteAiService create(Throwable cause) {
        log.error("[Feign降级] wu-service-ai 服务调用失败", cause);
        return new RemoteAiService() {
            @Override
            public R<AiChatLogDTO> getChatLogById(Long id) {
                return R.fail("AI 服务暂不可用");
            }
        };
    }
}
