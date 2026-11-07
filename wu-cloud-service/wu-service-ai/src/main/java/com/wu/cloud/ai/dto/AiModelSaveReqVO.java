package com.wu.cloud.ai.dto;

import lombok.Data;

/**
 * AI 模型配置新增/编辑入参
 * 编辑时 apiKey 传空表示保持原密钥不变
 */
@Data
public class AiModelSaveReqVO {
    private Long id;
    private String name;
    /** 供应商标识，仅用于展示与筛选，允许任意取值 */
    private String provider;
    /** 接入协议: openai / anthropic，留空默认 openai */
    private String protocol;
    private String modelName;
    private String baseUrl;
    /** 自定义请求路径模板，支持 {model} 占位 */
    private String pathTemplate;
    /** 额外请求头 JSON */
    private String extraHeaders;
    /** 明文 API Key，服务端 SM4-GCM 加密后入库；编辑时留空则不修改 */
    private String apiKey;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
    private Integer isDefault;
    private Integer status;
    private String remark;
}
