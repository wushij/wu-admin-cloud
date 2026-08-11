package com.wu.cloud.api.ai.feign;

import com.wu.cloud.api.ai.dto.AiChatLogDTO;
import com.wu.cloud.api.ai.feign.fallback.RemoteAiServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 远程 AI 服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteAiService", value = "wu-service-ai", path = "/inner/ai", fallbackFactory = RemoteAiServiceFallback.class)
public interface RemoteAiService {

    @GetMapping("/{id}")
    R<AiChatLogDTO> getChatLogById(@PathVariable("id") Long id);
}
