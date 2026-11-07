package com.wu.cloud.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话日志 DO（主键必须 AUTO，与旧版 sys_ai_chat_log 自增一致）
 * <p>
 * 显式声明 {@code @TableId(AUTO)}，避免继承场景下落到雪花 ID；
 * Long 序列化为字符串，防止前端 JS 精度丢失导致删除「对话日志不存在」。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_chat_log")
public class AiChatLogDO extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

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
