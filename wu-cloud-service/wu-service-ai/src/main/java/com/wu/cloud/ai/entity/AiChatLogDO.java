package com.wu.cloud.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话日志 DO（与单体对齐：主键沿用 BaseEntity 的 IdType.AUTO 自增）
 * <p>
 * 不要再声明 {@code @TableId private Long id}，否则会覆盖父类 AUTO，
 * 默认变成雪花 ID；前端 JS 精度丢失后删除会提示「对话日志不存在」。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_chat_log")
public class AiChatLogDO extends BaseEntity {

    private Long userId;
    private String username;
    private String conversationId;
    private Long modelId;
    private String provider;
    private String modelName;
    private String question;
    private String answer;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long durationMs;
    private Integer chatStatus;
    private String errorMsg;
    private String source;
    private String toolTrace;

    public String getPrompt() {
        return question;
    }

    public void setPrompt(String prompt) {
        this.question = prompt;
    }

    public String getResponse() {
        return answer;
    }

    public void setResponse(String response) {
        this.answer = response;
    }

    public Integer getTokenCount() {
        return totalTokens;
    }

    public void setTokenCount(Integer tokenCount) {
        this.totalTokens = tokenCount;
    }
}
