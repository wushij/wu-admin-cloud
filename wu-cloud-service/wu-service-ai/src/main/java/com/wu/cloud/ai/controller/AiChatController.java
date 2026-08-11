package com.wu.cloud.ai.controller;

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
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> conversations() {
        return R.ok(aiChatService.listConversations());
    }

    @GetMapping("/history")
    public R<List<AiChatLogDO>> history(@RequestParam("conversationId") String conversationId) {
        return R.ok(aiChatService.listHistory(conversationId));
    }

    @DeleteMapping("/conversation")
    public R<Boolean> deleteConversation(@RequestParam("conversationId") String conversationId) {
        return R.ok(aiChatService.deleteConversation(conversationId));
    }

    @PutMapping("/rename")
    public R<Boolean> rename(@RequestBody Map<String, String> body) {
        String conversationId = body.get("conversationId");
        String title = body.get("title");
        return R.ok(aiChatService.renameConversation(conversationId, title));
    }
}
