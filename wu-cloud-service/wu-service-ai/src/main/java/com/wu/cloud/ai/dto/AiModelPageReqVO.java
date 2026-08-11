package com.wu.cloud.ai.dto;

import com.wu.cloud.common.core.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型配置分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelPageReqVO extends PageParam {
    private String name;
    private String provider;
    private Integer status;
}
