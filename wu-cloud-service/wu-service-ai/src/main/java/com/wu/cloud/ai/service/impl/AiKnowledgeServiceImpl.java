package com.wu.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wu.cloud.ai.entity.AiKnowledgeDO;
import com.wu.cloud.ai.mapper.AiKnowledgeMapper;
import com.wu.cloud.ai.service.AiKnowledgeService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AiKnowledgeServiceImpl extends ServiceImpl<AiKnowledgeMapper, AiKnowledgeDO>
        implements AiKnowledgeService {

    private static final Set<String> CATEGORIES = Set.of("faq", "manual", "module", "other");

    @Override
    public PageResult<AiKnowledgeDO> page(PageParam pageParam, String keyword, String category, Integer status) {
        Page<AiKnowledgeDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<AiKnowledgeDO> wrapper = new LambdaQueryWrapper<AiKnowledgeDO>()
                .eq(category != null && !category.isBlank(), AiKnowledgeDO::getCategory, category)
                .eq(status != null, AiKnowledgeDO::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(AiKnowledgeDO::getTitle, keyword)
                        .or().like(AiKnowledgeDO::getKeywords, keyword))
                .orderByDesc(AiKnowledgeDO::getSort)
                .orderByDesc(AiKnowledgeDO::getUpdateTime);
        Page<AiKnowledgeDO> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public AiKnowledgeDO detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiKnowledgeDO knowledge) {
        validate(knowledge);
        knowledge.setId(null);
        applyDefaults(knowledge);
        save(knowledge);
        return knowledge.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(AiKnowledgeDO knowledge) {
        if (knowledge.getId() == null) {
            throw new BusinessException("知识ID不能为空");
        }
        getOrThrow(knowledge.getId());
        validate(knowledge);
        applyDefaults(knowledge);
        return updateById(knowledge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        getOrThrow(id);
        return removeById(id);
    }

    private AiKnowledgeDO getOrThrow(Long id) {
        AiKnowledgeDO knowledge = getById(id);
        if (knowledge == null) {
            throw new BusinessException(404, "知识条目不存在");
        }
        return knowledge;
    }

    private void validate(AiKnowledgeDO knowledge) {
        if (knowledge.getTitle() == null || knowledge.getTitle().isBlank()) {
            throw new BusinessException("知识标题不能为空");
        }
        if (knowledge.getContent() == null || knowledge.getContent().isBlank()) {
            throw new BusinessException("知识正文不能为空");
        }
        if (knowledge.getCategory() != null && !knowledge.getCategory().isBlank()
                && !CATEGORIES.contains(knowledge.getCategory())) {
            throw new BusinessException("分类只能为 faq/manual/module/other");
        }
    }

    private void applyDefaults(AiKnowledgeDO knowledge) {
        if (knowledge.getCategory() == null || knowledge.getCategory().isBlank()) {
            knowledge.setCategory("faq");
        }
        if (knowledge.getSort() == null) {
            knowledge.setSort(0);
        }
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
    }
}
