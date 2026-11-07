package com.wu.cloud.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.core.util.StrUtil;
import com.wu.cloud.ai.dto.AiChatStreamReqVO;
import com.wu.cloud.ai.dto.AiModelRespVO;
import com.wu.cloud.ai.service.AiChatService;
import com.wu.cloud.ai.service.AiModelService;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
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
 * 支持两种调用方式：
 * 1. GET  /api/ai/chat/stream?prompt=...   （旧/直连方式）
 * 2. POST /api/ai/chat/stream  body=messages[] （wu-admin-web 前端 AiChatStreamBody 方式）
 * 两者均返回 SSE 流，事件约定：delta / status / done / error（与前端解析逻辑对齐）。
 * </p>
 */
@Tag(name = "AI 流式对话")
@RestController
@RequestMapping("/api/ai")
public class AiStreamController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    @Resource
    private RemoteUserService remoteUserService;

    /**
     * 启用中的模型列表（供聊天窗口模型选择器使用）
     * 前端调用：GET /api/ai/chat/models（契约 /ai/chat/models，返回启用模型）
     */
    @Operation(summary = "消息操作")
    @GetMapping("/chat/models")
    public R<List<AiModelRespVO>> chatModels() {
        return R.ok(aiModelService.listEnabled());
    }

    /**
     * GET 入口：通过 query 传 prompt
     */
    @Operation(summary = "多引擎文件流式读取与预览")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam("prompt") String prompt,
                                 @RequestParam(value = "conversationId", required = false) String conversationId) {
        if (!dynamicConfigHelper.isAiAssistantEnabled()) {
            throw new BusinessException("AI 助手功能已关闭");
        }
        AiChatStreamReqVO req = new AiChatStreamReqVO();
        req.setConversationId(conversationId);
        AiChatStreamReqVO.Message msg = new AiChatStreamReqVO.Message();
        msg.setRole("user");
        msg.setContent(prompt);
        req.setMessages(List.of(msg));
        return aiChatService.stream(req, requireUserId(), resolveUsername());
    }

    /**
     * POST 入口（wu-admin-web 前端 AiChatStreamBody 方式）：
     * 请求体为 messages 数组，交给真实 Provider 流式对话。
     */
    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatPost(@RequestBody AiChatStreamReqVO req) {
        if (!dynamicConfigHelper.isAiAssistantEnabled()) {
            throw new BusinessException("AI 助手功能已关闭");
        }
        return aiChatService.stream(req, requireUserId(), resolveUsername());
    }

    /** 未登录禁止对话，避免 userId=0 时空上下文导致模型编造身份 */
    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(401, "登录已过期，请重新登录后使用 AI 助手");
        }
        return userId;
    }

    /**
     * 优先用网关透传的用户名；缺失时经 Feign 向用户服务回查，保证日志必有 username。
     */
    private String resolveUsername() {
        String username = UserContextHolder.getUsername();
        if (StrUtil.isNotBlank(username)) {
            return username.trim();
        }
        Long userId = UserContextHolder.getUserId();
        if (userId == null || userId <= 0) {
            return "";
        }
        try {
            R<SysUserDTO> resp = remoteUserService.getUserById(userId);
            if (resp != null && resp.isSuccess() && resp.getData() != null
                    && StrUtil.isNotBlank(resp.getData().getUsername())) {
                return resp.getData().getUsername().trim();
            }
        } catch (Exception e) {
            // 用户服务不可用时保底返回空串，不阻断会话主流程
        }
        return "";
    }
}
