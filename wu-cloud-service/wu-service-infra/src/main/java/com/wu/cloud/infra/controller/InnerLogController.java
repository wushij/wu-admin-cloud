package com.wu.cloud.infra.controller;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.service.OperLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志内部端点（历史实现，写入 {@code infra_oper_log}）。
 * <p>
 * 管理端「操作日志」已统一读 {@code sys_oper_log}，{@link com.wu.cloud.api.system.feign.RemoteLogService}
 * 已切到 {@code wu-service-system}。本接口仅保留兼容，避免旧调用方直接 404。
 * </p>
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
