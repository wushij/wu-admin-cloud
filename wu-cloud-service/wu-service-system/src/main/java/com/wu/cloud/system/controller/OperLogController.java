package com.wu.cloud.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysOperLogDO;
import com.wu.cloud.system.mapper.OperLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志 REST（对外唯一入口，对齐 FE /system/oper-log）
 */
@RestController
@RequestMapping("/api/system/oper-log")
public class OperLogController {

    @Resource
    private OperLogMapper operLogMapper;

    @RequiresPermissions("system:operlog:list")
    @GetMapping("/page")
    public R<PageResult<SysOperLogDO>> getOperLogPage(PageQuery pageQuery,
                                                      @RequestParam(value = "title", required = false) String title,
                                                      @RequestParam(value = "operName", required = false) String operName,
                                                      @RequestParam(value = "status", required = false) Integer status) {
        int pageNo = pageQuery.getPageNo() == null ? 1 : pageQuery.getPageNo();
        int pageSize = pageQuery.getPageSize() == null ? 10 : pageQuery.getPageSize();
        Page<SysOperLogDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysOperLogDO> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            wrapper.like(SysOperLogDO::getTitle, title);
        }
        if (operName != null && !operName.isBlank()) {
            wrapper.like(SysOperLogDO::getOperName, operName);
        }
        if (status != null) {
            wrapper.eq(SysOperLogDO::getStatus, status);
        }
        wrapper.orderByDesc(SysOperLogDO::getOperTime);
        Page<SysOperLogDO> result = operLogMapper.selectPage(page, wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @RequiresPermissions("system:operlog:remove")
    @Log(title = "操作日志", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        operLogMapper.deleteById(id);
        return R.ok(true);
    }

    @RequiresPermissions("system:operlog:remove")
    @Log(title = "操作日志", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/clean")
    public R<Boolean> clean() {
        operLogMapper.delete(new LambdaQueryWrapper<>());
        return R.ok(true);
    }

    /** 内部/兼容写入 */
    @PostMapping("/create")
    public R<Boolean> createOperLog(@RequestBody SysOperLogDO operLogDO) {
        operLogMapper.insert(operLogDO);
        return R.ok(true);
    }
}
