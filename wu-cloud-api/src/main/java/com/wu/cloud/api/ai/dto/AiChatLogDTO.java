package com.wu.cloud.api.ai.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 聊天记录 DTO (跨服务 RPC 传输)
 */
@Data
public class AiChatLogDTO implements Serializable {

    private Long id;
    private Long userId;
    private String conversationId;
    private String question;
    private String answer;
    private Integer chatStatus;
    private LocalDateTime createTime;
}
