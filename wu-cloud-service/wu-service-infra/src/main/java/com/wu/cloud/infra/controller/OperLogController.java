package com.wu.cloud.infra.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.OperLogDO;
import com.wu.cloud.infra.service.OperLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志查询 REST 控制器
 */
@RestController
@RequestMapping("/api/infra/oper-log")
public class OperLogController {

    @Resource
    private OperLogService operLogService;

    @GetMapping("/recent")
    public R<List<OperLogDO>> recent(@RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return R.ok(operLogService.listRecent(limit));
    }
}
