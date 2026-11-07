package com.wu.cloud.ai.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.service.AiChatService;
import com.wu.cloud.api.ai.dto.AiChatLogDTO;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * AI 服务内部 RPC 端点。
 *
 * <p>契约对齐：返回值统一使用 {@code AiChatLogDTO}，与 wu-cloud-api 中
 * {@code RemoteAiService} 声明一致。AiChatLogDO 额外携带 provider、
 * modelName、tokens、toolTrace 等内部字段，不应跨服务外泄。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/ai")
public class InnerAiController {

    @Resource
    private AiChatService aiChatService;

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<AiChatLogDTO> getChatLogById(@PathVariable("id") Long id) {
        return R.ok(toDTO(aiChatService.getChatLogById(id)));
    }

    private AiChatLogDTO toDTO(AiChatLogDO log) {
        if (log == null) {
            return null;
        }
        AiChatLogDTO dto = new AiChatLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setConversationId(log.getConversationId());
        dto.setQuestion(log.getQuestion());
        dto.setAnswer(log.getAnswer());
        dto.setChatStatus(log.getChatStatus());
        dto.setCreateTime(log.getCreateTime());
        return dto;
    }
}
