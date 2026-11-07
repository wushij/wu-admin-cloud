package com.wu.cloud.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 知识库批量更新状态入参
 */
@Data
public class AiKnowledgeBatchStatusReqVO {

    @NotEmpty(message = "知识ID列表不能为空")
    private List<Long> ids;

    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
