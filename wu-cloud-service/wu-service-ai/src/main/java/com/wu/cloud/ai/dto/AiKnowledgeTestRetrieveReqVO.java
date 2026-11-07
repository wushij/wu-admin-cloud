package com.wu.cloud.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 知识库检索测试入参
 */
@Data
public class AiKnowledgeTestRetrieveReqVO {

    @NotBlank(message = "测试检索提问不能为空")
    private String query;
}
