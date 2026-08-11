package com.wu.cloud.ai.controller;

import com.wu.cloud.ai.dto.AiChatStreamReqVO;
import com.wu.cloud.ai.dto.AiModelRespVO;
import com.wu.cloud.ai.service.AiChatService;
import com.wu.cloud.ai.service.AiModelService;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 智能助手 SSE 流式输出控制层 (路由映射 /api/ai/**)
 * <p>
 * 兼容两种调用方式：
 * 1. GET  /api/ai/chat/stream?prompt=...   （旧/直连方式）
 * 2. POST /api/ai/chat/stream  body=messages[] （wu-admin-web 前端 AiChatStreamBody 方式）
 * 两者均返回 SSE 流，事件约定：delta / status / done / error（与前端解析逻辑对齐）。
 * </p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiStreamController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private com.wu.cloud.common.config.DynamicConfigHelper dynamicConfigHelper;

    /**
     * 启用中的模型列表（供聊天窗口模型选择器使用）
     * 前端调用：GET /api/ai/chat/models（契约 /ai/chat/models，返回启用模型）
     */
    @GetMapping("/chat/models")
    public R<List<AiModelRespVO>> chatModels() {
        return R.ok(aiModelService.listEnabled());
    }

    /**
     * GET 兼容入口（旧调用方）：通过 query 传 prompt
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam("prompt") String prompt,
                                 @RequestParam(value = "conversationId", required = false) String conversationId) {
        if (!dynamicConfigHelper.isAiAssistantEnabled()) {
            throw new com.wu.cloud.common.exception.BusinessException("AI 助手功能已关闭");
        }
        AiChatStreamReqVO req = new AiChatStreamReqVO();
        req.setConversationId(conversationId);
        AiChatStreamReqVO.Message msg = new AiChatStreamReqVO.Message();
        msg.setRole("user");
        msg.setContent(prompt);
        req.setMessages(List.of(msg));
        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();
        return aiChatService.stream(req, userId, username);
    }

    /**
     * POST 入口（wu-admin-web 前端 AiChatStreamBody 方式，P0-2 修复）：
     * 请求体为 messages 数组，交给真实 Provider 流式对话。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(@RequestBody AiChatStreamReqVO req) {
        if (!dynamicConfigHelper.isAiAssistantEnabled()) {
            throw new com.wu.cloud.common.exception.BusinessException("AI 助手功能已关闭");
        }
        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();
        return aiChatService.stream(req, userId, username);
    }
}
