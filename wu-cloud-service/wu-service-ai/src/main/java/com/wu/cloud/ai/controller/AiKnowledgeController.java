package com.wu.cloud.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.ai.dto.AiKnowledgeBatchStatusReqVO;
import com.wu.cloud.ai.dto.AiKnowledgePageReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeRespVO;
import com.wu.cloud.ai.dto.AiKnowledgeSaveReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeTestRetrieveReqVO;
import com.wu.cloud.ai.dto.AiKnowledgeTestRetrieveRespVO;
import com.wu.cloud.ai.service.AiKnowledgeService;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库管理 REST 控制器（与最新业务规范能力对齐）
 * <p>规范路径：/api/ai/knowledge/*。</p>
 */
@Tag(name = "AI 知识库")
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<AiKnowledgeRespVO>> page(AiKnowledgePageReqVO reqVO) {
        return R.ok(aiKnowledgeService.page(reqVO));
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<AiKnowledgeRespVO> detail(@PathVariable("id") Long id) {
        return R.ok(aiKnowledgeService.detail(id));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Boolean> create(@Valid @RequestBody AiKnowledgeSaveReqVO reqVO) {
        aiKnowledgeService.create(reqVO);
        return R.ok(true, "新增知识成功");
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@Valid @RequestBody AiKnowledgeSaveReqVO reqVO) {
        aiKnowledgeService.update(reqVO);
        return R.ok(true, "修改知识成功");
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        aiKnowledgeService.delete(id);
        return R.ok(true, "删除知识成功");
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "连通性测试")
    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        aiKnowledgeService.updateStatus(id, status);
        return R.ok(true, "修改知识状态成功");
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "批量操作")
    @PutMapping("/batch-status")
    public R<Boolean> batchUpdateStatus(@Valid @RequestBody AiKnowledgeBatchStatusReqVO reqVO) {
        aiKnowledgeService.batchUpdateStatus(reqVO.getIds(), reqVO.getStatus());
        return R.ok(true, "批量修改知识状态成功");
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "批量删除短信发送记录")
    @DeleteMapping("/batch")
    public R<Boolean> batchDelete(@RequestBody List<Long> ids) {
        aiKnowledgeService.batchDelete(ids);
        return R.ok(true, "批量删除知识成功");
    }

    /** 知识库检索演练与召回测试 */
    @Operation(summary = "连通性测试")
    @PostMapping("/test-retrieve")
    public R<AiKnowledgeTestRetrieveRespVO> testRetrieve(@Valid @RequestBody AiKnowledgeTestRetrieveReqVO reqVO) {
        return R.ok(aiKnowledgeService.testRetrieve(reqVO));
    }

    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "批量操作")
    @PostMapping("/batch-import")
    public R<Boolean> batchImport(@RequestBody List<AiKnowledgeSaveReqVO> list) {
        aiKnowledgeService.batchImport(list);
        return R.ok(true, "批量导入知识成功");
    }

    /** 上传 Markdown (.md) 文档 */
    @Log(title = "AI知识库", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "上传文件")
    @PostMapping("/upload-md")
    public R<Map<String, Object>> uploadMd(@RequestParam("files") MultipartFile[] files) {
        return R.ok(aiKnowledgeService.uploadMd(files));
    }

    @Operation(summary = "Redis 实时统计（图表）")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(aiKnowledgeService.getStats());
    }

    @Operation(summary = "导出 API 访问日志")
    @GetMapping("/export")
    public R<List<AiKnowledgeRespVO>> exportAll() {
        return R.ok(aiKnowledgeService.exportAll());
    }
}
