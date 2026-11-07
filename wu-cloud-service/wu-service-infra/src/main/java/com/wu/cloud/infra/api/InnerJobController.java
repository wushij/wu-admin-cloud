package com.wu.cloud.infra.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.service.SysJobService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 定时任务内部统计（供 BFF 工作台聚合）
 */
@Hidden
@RestController
@RequestMapping("/inner/job")
public class InnerJobController {

    @Resource
    private SysJobService sysJobService;

    @Operation(summary = "获取统计信息")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(sysJobService.overview());
    }
}
