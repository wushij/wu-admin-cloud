package com.wu.cloud.ai.controller;

import com.wu.cloud.ai.entity.AiKnowledgeDO;
import com.wu.cloud.ai.service.AiKnowledgeService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * AI 知识库（对齐前端 POST/PUT /system/ai-knowledge、DELETE /{id}）
 */
@RestController
@RequestMapping({"/api/ai/knowledge", "/api/system/ai-knowledge"})
public class AiKnowledgeController {

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    @GetMapping("/page")
    public R<PageResult<AiKnowledgeDO>> page(PageParam pageParam,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "category", required = false) String category,
                                             @RequestParam(value = "status", required = false) Integer status) {
        return R.ok(aiKnowledgeService.page(pageParam, keyword, category, status));
    }

    @GetMapping("/{id}")
    public R<AiKnowledgeDO> detail(@PathVariable("id") Long id) {
        return R.ok(aiKnowledgeService.detail(id));
    }

    @PostMapping
    public R<Long> create(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.create(knowledge));
    }

    @PostMapping("/create")
    public R<Long> createCompat(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.create(knowledge));
    }

    @PutMapping
    public R<Boolean> update(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.update(knowledge));
    }

    @PostMapping("/update")
    public R<Boolean> updateCompat(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.update(knowledge));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        return R.ok(aiKnowledgeService.delete(id));
    }

    @PostMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        return R.ok(aiKnowledgeService.delete(id));
    }
}