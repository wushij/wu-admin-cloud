package com.wu.cloud.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.service.SysConfigGroupService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：供 auth 等无 JDBC 服务获取系统配置
 */
@Hidden
@RestController
@RequestMapping("/inner/system/config")
public class InnerConfigController {

    @Resource
    private SysConfigGroupService sysConfigGroupService;

    @Operation(summary = "获取详情")
    @GetMapping("/get-group")
    public R<SysConfigGroupDO> getGroup(@RequestParam("groupCode") String groupCode) {
        return R.ok(sysConfigGroupService.getConfigGroupByCode(groupCode));
    }
}
