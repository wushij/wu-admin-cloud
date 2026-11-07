package com.wu.cloud.ai.service;

import com.wu.cloud.ai.dto.AiKnowledgePageReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeRespVO;
import com.wu.cloud.ai.dto.AiKnowledgeSaveReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeTestRetrieveReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeTestRetrieveRespVO;
import com.wu.cloud.common.core.domain.PageResult;

import java.util.List;

/**
 * AI 知识库管理（L2 RAG-lite）
 */
public interface AiKnowledgeService {

    PageResult<AiKnowledgeRespVO> page(AiKnowledgePageReqVO reqVO);

    AiKnowledgeRespVO detail(Long id);

    void create(AiKnowledgeSaveReqVO reqVO);

    void update(AiKnowledgeSaveReqVO reqVO);

    void delete(Long id);

    /** 单条修改启用/停用状态 */
    void updateStatus(Long id, Integer status);

    /** 批量修改启用/停用状态 */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /** 批量删除知识条目 */
    void batchDelete(List<Long> ids);

    /** 检索测试与命中仿真 */
    AiKnowledgeTestRetrieveRespVO testRetrieve(AiKnowledgeTestRetrieveReqVO reqVO);

    /** 批量导入知识条目 */
    void batchImport(List<AiKnowledgeSaveReqVO> list);

    /** 上传 Markdown (.md) 文件批量解析并入库 */
    java.util.Map<String, Object> uploadMd(org.springframework.web.multipart.MultipartFile[] files);

    /** 获取知识库统计指标（总数、启用数、停用数、总字数） */
    java.util.Map<String, Object> getStats();

    /** 导出全量知识列表 */
    List<AiKnowledgeRespVO> exportAll();
}
