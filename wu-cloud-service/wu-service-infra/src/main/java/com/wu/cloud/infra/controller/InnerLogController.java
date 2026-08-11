package com.wu.cloud.infra.controller;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.service.OperLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志内部 RPC 端点（实现 RemoteLogService 契约，由网关 403 黑名单保护）
 */
@RestController
@RequestMapping("/inner/log")
public class InnerLogController {

    @Resource
    private OperLogService operLogService;

    @PostMapping("/saveOperLog")
    public R<Boolean> saveOperLog(@RequestBody OperLogDTO operLogDTO) {
        operLogService.saveOperLog(operLogDTO);
        return R.ok(true);
    }
}
