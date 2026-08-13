package com.wu.cloud.ai.controller;

import com.wu.cloud.ai.entity.AiKnowledgeDO;
import com.wu.cloud.ai.service.AiKnowledgeService;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * AI 知识库管理 REST 控制器
 * <p>规范路径：/api/ai/knowledge/*（P1 已去掉 /api/system/ai-knowledge 兼容映射）。</p>
 */
@RestController
@RequestMapping("/api/ai/knowledge")
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

    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.create(knowledge));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Long> createCompat(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.create(knowledge));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> update(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.update(knowledge));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Boolean> updateCompat(@RequestBody AiKnowledgeDO knowledge) {
        return R.ok(aiKnowledgeService.update(knowledge));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        return R.ok(aiKnowledgeService.delete(id));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Boolean> deleteCompat(@RequestParam("id") Long id) {
        return R.ok(aiKnowledgeService.delete(id));
    }
}
