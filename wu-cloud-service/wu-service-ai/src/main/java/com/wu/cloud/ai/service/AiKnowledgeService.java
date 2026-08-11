package com.wu.cloud.ai.service;

import com.wu.cloud.ai.entity.AiKnowledgeDO;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;

public interface AiKnowledgeService {

    PageResult<AiKnowledgeDO> page(PageParam pageParam, String keyword, String category, Integer status);

    AiKnowledgeDO detail(Long id);

    Long create(AiKnowledgeDO knowledge);

    boolean update(AiKnowledgeDO knowledge);

    boolean delete(Long id);
}
