package com.wu.cloud.ai.controller;

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
 * <p>契约对齐 wu-admin-web/src/api/system/ai-model/index.ts（/system/ai-model/*）。</p>
 */
@RestController
@RequestMapping({"/api/system/ai-model", "/api/ai/model"})
public class AiModelController {

    @Resource
    private AiModelService aiModelService;

    @GetMapping("/page")
    public R<PageResult<AiModelRespVO>> page(AiModelPageReqVO reqVO) {
        return R.ok(aiModelService.page(reqVO));
    }

    /** 启用中的模型列表（对话面板模型选择器，契约 /ai/chat/models） */
    @GetMapping({"/enabled", "/list"})
    public R<List<AiModelRespVO>> listEnabled() {
        return R.ok(aiModelService.listEnabled());
    }

    @GetMapping("/{id}")
    public R<AiModelRespVO> detail(@PathVariable("id") Long id) {
        return R.ok(aiModelService.detail(id));
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.INSERT)
    @PostMapping
    public R<Boolean> create(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.create(reqVO);
        return R.ok(true, "保存模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public R<Boolean> update(@RequestBody AiModelSaveReqVO reqVO) {
        aiModelService.update(reqVO);
        return R.ok(true, "修改模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        aiModelService.delete(id);
        return R.ok(true, "删除模型配置成功");
    }

    @Log(title = "AI模型配置", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/{id}/default")
    public R<Boolean> setDefault(@PathVariable("id") Long id) {
        aiModelService.setDefault(id);
        return R.ok(true, "设为默认模型成功");
    }

    @PostMapping("/test")
    public R<Long> test(@RequestBody AiModelTestReqVO reqVO) {
        return R.ok(aiModelService.test(reqVO));
    }
}
