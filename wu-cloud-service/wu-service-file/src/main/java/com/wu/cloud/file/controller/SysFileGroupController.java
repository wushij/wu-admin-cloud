package com.wu.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.file.entity.SysFileGroupDO;
import com.wu.cloud.file.service.SysFileGroupService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文件分组 REST（路径对齐 wu-admin-web /infra/file-group）
 */
@Tag(name = "文件分组")
@RestController
@RequestMapping("/api/infra/file-group")
public class SysFileGroupController {

    @Resource
    private SysFileGroupService sysFileGroupService;

    @RequiresPermissions("sys:file:list")
    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestParam(value = "fileCategory", required = false) String fileCategory) {
        return R.ok(sysFileGroupService.listWithUngroupedCount(fileCategory));
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.INSERT)
    @RequiresPermissions("sys:file:upload")
    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Boolean> create(@RequestBody SysFileGroupDO group) {
        sysFileGroupService.createGroup(group);
        return R.ok(true);
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.UPDATE)
    @RequiresPermissions("sys:file:upload")
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody SysFileGroupDO group) {
        sysFileGroupService.updateGroup(group);
        return R.ok(true);
    }

    @Log(title = "文件分组", businessType = Log.BusinessType.DELETE)
    @RequiresPermissions("sys:file:delete")
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        sysFileGroupService.deleteGroup(id);
        return R.ok(true);
    }
}
