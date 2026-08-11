package com.wu.cloud.ai.controller;

import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.service.AiChatLogService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 对话日志管理 REST 控制器（对齐单体 DELETE /{id}、DELETE /clean）
 */
@RestController
@RequestMapping({"/api/ai/log", "/api/system/ai-log"})
public class AiChatLogController {

    @Resource
    private AiChatLogService aiChatLogService;

    @GetMapping("/page")
    public R<PageResult<AiChatLogDO>> page(PageParam pageParam,
                                           @RequestParam(value = "username", required = false) String username,
                                           @RequestParam(value = "provider", required = false) String provider,
                                           @RequestParam(value = "chatStatus", required = false) Integer chatStatus,
                                           @RequestParam(value = "conversationId", required = false) String conversationId) {
        return R.ok(aiChatLogService.page(pageParam, username, provider, chatStatus, conversationId));
    }

    /** 对齐前端 del(`/system/ai-log/${id}`) / 单体 DELETE /{id} */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        return R.ok(aiChatLogService.delete(id));
    }

    /** 对齐前端 del('/system/ai-log/clean') / 单体 DELETE /clean */
    @DeleteMapping("/clean")
    public R<Boolean> clean() {
        return R.ok(aiChatLogService.cleanLogs());
    }

    @PostMapping("/delete")
    public R<Boolean> deleteByPost(@RequestParam("id") Long id) {
        return R.ok(aiChatLogService.delete(id));
    }

    @PostMapping("/clean")
    public R<Boolean> cleanByPost() {
        return R.ok(aiChatLogService.cleanLogs());
    }

    @GetMapping("/stats/tokens")
    public R<Map<String, Object>> tokenStats(@RequestParam(value = "startDate", required = false) String startDate,
                                             @RequestParam(value = "endDate", required = false) String endDate) {
        return R.ok(aiChatLogService.tokenStats(startDate, endDate));
    }
}
