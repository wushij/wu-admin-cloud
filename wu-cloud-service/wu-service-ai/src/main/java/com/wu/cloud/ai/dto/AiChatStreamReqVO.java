package com.wu.cloud.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 流式对话请求体（与 wu-admin-web/src/api/ai/index.ts 的 AiChatStreamBody 对齐）
 * <p>
 * 前端通过 POST /api/ai/chat/stream 提交，消息以 messages 数组承载；
 * 后端从中提取最后一条用户消息作为 prompt，返回 SSE 流。
 * </p>
 */
@Data
public class AiChatStreamReqVO {

    /** 会话 ID */
    private String conversationId;

    /** 模型 ID */
    private Long modelId;

    /** 来源：pc / mobile */
    private String source;

    /** 对话消息序列（role: user / assistant, content: 文本） */
    private List<Message> messages;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
