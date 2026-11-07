package com.wu.cloud.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.service.AiChatService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 会话管理与历史问答 REST 控制器
 */
@Tag(name = "AI wu助手对话")
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @Operation(summary = "获取详情")
    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> conversations() {
        return R.ok(aiChatService.listConversations());
    }

    @Operation(summary = "获取详情")
    @GetMapping("/history")
    public R<List<AiChatLogDO>> history(@RequestParam("conversationId") String conversationId) {
        return R.ok(aiChatService.listHistory(conversationId));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/conversation")
    public R<Boolean> deleteConversation(@RequestParam("conversationId") String conversationId) {
        return R.ok(aiChatService.deleteConversation(conversationId));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/conversations")
    public R<Boolean> clearConversations() {
        return R.ok(aiChatService.clearConversations());
    }

    @Operation(summary = "重命名")
    @PutMapping("/rename")
    public R<Boolean> rename(@RequestBody Map<String, String> body) {
        String conversationId = body.get("conversationId");
        String title = body.get("title");
        return R.ok(aiChatService.renameConversation(conversationId, title));
    }
}
