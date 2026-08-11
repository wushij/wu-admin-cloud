package com.wu.cloud.ai.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiChatLogVO implements Serializable {
    private Long id;
    private Long userId;
    private String conversationId;
    private String question;
    private String answer;
    private LocalDateTime createTime;
}
