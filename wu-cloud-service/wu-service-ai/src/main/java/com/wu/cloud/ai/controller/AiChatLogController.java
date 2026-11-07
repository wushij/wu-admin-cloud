package com.wu.cloud.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.service.AiChatLogService;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 对话日志管理 REST 控制器
 * <p>规范路径：/api/ai/log/*（旧前缀 /api/system/ai-log 已下线）。</p>
 */
@Tag(name = "AI 对话日志")
@RestController
@RequestMapping("/api/ai/log")
public class AiChatLogController {

    @Resource
    private AiChatLogService aiChatLogService;

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<AiChatLogDO>> page(PageParam pageParam,
                                           @RequestParam(value = "username", required = false) String username,
                                           @RequestParam(value = "provider", required = false) String provider,
                                           @RequestParam(value = "chatStatus", required = false) Integer chatStatus,
                                           @RequestParam(value = "conversationId", required = false) String conversationId) {
        return R.ok(aiChatLogService.page(pageParam, username, provider, chatStatus, conversationId));
    }

    /** 对齐前端 del(`/system/ai-log/${id}`) / 旧版 DELETE /{id} */
    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        return R.ok(aiChatLogService.delete(id));
    }

    /** 对齐前端 del('/system/ai-log/clean') / 旧版 DELETE /clean */
    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/clean")
    public R<Boolean> clean() {
        return R.ok(aiChatLogService.cleanLogs());
    }

    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除")
    @PostMapping("/delete")
    public R<Boolean> deleteByPost(@RequestParam("id") Long id) {
        return R.ok(aiChatLogService.delete(id));
    }

    @Log(title = "AI对话日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "清理")
    @PostMapping("/clean")
    public R<Boolean> cleanByPost() {
        return R.ok(aiChatLogService.cleanLogs());
    }

    @Operation(summary = "获取详情")
    @GetMapping("/stats/tokens")
    public R<Map<String, Object>> tokenStats(@RequestParam(value = "startDate", required = false) String startDate,
                                             @RequestParam(value = "endDate", required = false) String endDate) {
        return R.ok(aiChatLogService.tokenStats(startDate, endDate));
    }
}
