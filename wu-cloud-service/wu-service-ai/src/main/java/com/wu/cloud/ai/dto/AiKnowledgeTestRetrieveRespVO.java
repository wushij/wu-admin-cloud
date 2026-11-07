package com.wu.cloud.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 知识库检索测试出参
 */
@Data
public class AiKnowledgeTestRetrieveRespVO {

    /** 检索测试提问内容 */
    private String query;

    /** 拆解出的分词/检索词列表 */
    private List<String> searchTerms;

    /** 匹配模式: FULLTEXT(全文ngram检索) / LIKE(关键词降级匹配) / NONE(未命中) */
    private String matchMode;

    /** 命中的知识条目列表 */
    private List<AiKnowledgeRespVO> hitList;

    /** 最终拼装成的大模型 Prompt 注入参考资料块 */
    private String referenceBlock;

    /** 注入字符总数 */
    private Integer charCount;

    /** 估算消耗的 Token 数量 (约字符数 / 1.5) */
    private Integer estimatedTokens;
}
