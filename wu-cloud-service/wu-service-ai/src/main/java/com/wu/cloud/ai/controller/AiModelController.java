package com.wu.cloud.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.ai.dto.AiModelListModelsReqVO;
import com.wu.cloud.ai.dto.AiModelPageReqVO;
import com.wu.cloud.ai.dto.AiModelRespVO;
import com.wu.cloud.ai.dto.AiModelSaveReqVO;
import com.wu.cloud.ai.dto.AiModelTestReqVO;
import com.wu.cloud.ai.service.AiModelService;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型配置管理 REST 控制器
 * <p>规范路径：/api/ai/model/*（旧前缀 /api/system/ai-model 已下线）。</p>
 */
@Tag(name = "AI 模型配置")
@RestController
@RequestMapping("/api/ai/model")
public class AiModelController {

    @Resource
    private AiModelService aiModelService;

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<AiModelRespVO>> page(AiModelPageReqVO reqVO) {
        return R.ok(aiModelService.page(reqVO));
    }

    /** 启用中的模型列表（对话面板模型选择器，契约 /ai/chat/models） */
    @Operation(summary = "多引擎文件流式读取与预览")
    @GetMapping({"/enabled", "/list"})
    public R<List<AiModelRespVO>> listEnabled() {
        return R.ok(aiModelService.listEnabled());
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<AiModelRespVO> detail(@PathVariable("id") Long id) {
        return R.ok(aiModelService.detail(id));
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Boolean> create(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.create(reqVO);
        return R.ok(true, "保存模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.update(reqVO);
        return R.ok(true, "修改模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        aiModelService.delete(id);
        return R.ok(true, "删除模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public R<Boolean> setDefault(@PathVariable("id") Long id) {
        aiModelService.setDefault(id);
        return R.ok(true, "设为默认模型成功");
    }

    @Operation(summary = "连通性测试")
    @PostMapping("/test")
    public R<Long> test(@RequestBody AiModelTestReqVO reqVO) {
        return R.ok(aiModelService.test(reqVO));
    }

    /** 拉取供应商可用模型列表（配置页「获取模型」） */
    @Operation(summary = "模型操作")
    @PostMapping("/models")
    public R<List<String>> models(@RequestBody AiModelListModelsReqVO reqVO) {
        return R.ok(aiModelService.listModels(reqVO));
    }
}
