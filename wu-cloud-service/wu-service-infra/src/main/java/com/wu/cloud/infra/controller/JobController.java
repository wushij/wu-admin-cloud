package com.wu.cloud.infra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.SysJobDO;
import com.wu.cloud.infra.entity.SysJobLogDO;
import com.wu.cloud.infra.service.SysJobLogService;
import com.wu.cloud.infra.service.SysJobService;
import com.wu.cloud.infra.vo.JobTemplateVO;
import com.wu.cloud.infra.vo.SysJobVO;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理 REST 控制器。
 * <p>C 队修复：从内存态 TaskScheduler 升级为 DB（sys_job/sys_job_log）完整 CRUD + 日志 + 回收站。
 * <p>路径说明：前端直接调用 {@code /api/infra/job/**}（网关路由 {@code Path=/api/infra/**}），
 * 不依赖任何网关重写规则。</p>
 */
@Tag(name = "定时任务")
@RestController
@RequestMapping("/api/infra/job")
public class JobController {

    @Resource
    private SysJobService sysJobService;
    @Resource
    private SysJobLogService sysJobLogService;

    @Operation(summary = "获取统计信息")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(sysJobService.overview());
    }

    @Operation(summary = "获取详情")
    @GetMapping("/templates")
    public R<List<JobTemplateVO>> templates() {
        return R.ok(sysJobService.templates());
    }

    @Operation(summary = "校验")
    @GetMapping("/checkCron")
    public R<Map<String, Object>> checkCron(@RequestParam String cronExpression) {
        return R.ok(sysJobService.checkCron(cronExpression));
    }

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<SysJobVO>> page(@RequestParam(defaultValue = "1") Integer pageNo,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String jobName,
                                        @RequestParam(required = false) String jobGroup,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(sysJobService.pageVo(pageNo, pageSize, jobName, jobGroup, status));
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<SysJobDO> detail(@PathVariable Long id) {
        return R.ok(sysJobService.getById(id));
    }

    @Operation(summary = "上传工单附件")
    @PostMapping
    public R<Boolean> create(@RequestBody SysJobDO job) {
        sysJobService.create(job);
        return R.ok(true);
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody SysJobDO job) {
        sysJobService.update(job);
        return R.ok(true);
    }

    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        sysJobService.delete(id);
        return R.ok(true);
    }

    @Operation(summary = "修改状态")
    @PutMapping("/changeStatus")
    public R<Boolean> changeStatus(@RequestBody StatusRequest request) {
        sysJobService.changeStatus(request.getId(), request.getStatus());
        return R.ok(true);
    }

    @Operation(summary = "执行任务")
    @PostMapping("/run/{id}")
    public R<Boolean> run(@PathVariable Long id) {
        sysJobService.run(id);
        return R.ok(true);
    }

    // ===== 调度日志 =====

    @Operation(summary = "获取统计数据")
    @GetMapping("/log/statistics")
    public R<Map<String, Object>> logStatistics() {
        return R.ok(sysJobLogService.statistics());
    }

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/log/page")
    public R<PageResult<SysJobLogDO>> logPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String jobName,
                                              @RequestParam(required = false) String jobGroup,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(sysJobLogService.page(pageNo, pageSize, jobName, jobGroup, status));
    }

    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/log/clean")
    public R<Boolean> cleanLog(@RequestParam(required = false) String jobName,
                               @RequestParam(required = false) String jobGroup) {
        if (jobName != null && !jobName.isBlank()) {
            sysJobLogService.cleanScope(jobName, jobGroup);
        } else {
            sysJobLogService.cleanAll();
        }
        return R.ok(true);
    }

    // ===== 回收站 =====

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/log/recycle/page")
    public R<PageResult<SysJobLogDO>> logRecyclePage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String jobName,
                                                     @RequestParam(required = false) String jobGroup) {
        return R.ok(sysJobLogService.recyclePage(pageNo, pageSize, jobName, jobGroup));
    }

    @Operation(summary = "恢复菜单")
    @PutMapping("/log/restore")
    public R<Boolean> restoreLog(@RequestParam Long id) {
        sysJobLogService.restore(id);
        return R.ok(true);
    }

    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/log/delete-permanent")
    public R<Boolean> deleteLogPermanent(@RequestParam Long id) {
        sysJobLogService.deletePermanent(id);
        return R.ok(true);
    }

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<SysJobDO>> recyclePage(@RequestParam(defaultValue = "1") Integer pageNo,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String jobName,
                                               @RequestParam(required = false) String jobGroup) {
        return R.ok(sysJobService.recyclePage(pageNo, pageSize, jobName, jobGroup));
    }

    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam Long id) {
        sysJobService.restore(id);
        return R.ok(true);
    }

    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam Long id) {
        sysJobService.deletePermanent(id);
        return R.ok(true);
    }

    @Data
    public static class StatusRequest {
        private Long id;
        private Integer status;
    }
}
