package com.wu.cloud.infra.controller;

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
 * 网关 compat-monitor-job 将前端 /system/monitor/job/** 重写为 /api/infra/job/**。</p>
 */
@RestController
@RequestMapping("/api/infra/job")
public class JobController {

    @Resource
    private SysJobService sysJobService;
    @Resource
    private SysJobLogService sysJobLogService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(sysJobService.overview());
    }

    @GetMapping("/templates")
    public R<List<JobTemplateVO>> templates() {
        return R.ok(sysJobService.templates());
    }

    @GetMapping("/checkCron")
    public R<Map<String, Object>> checkCron(@RequestParam String cronExpression) {
        return R.ok(sysJobService.checkCron(cronExpression));
    }

    @GetMapping("/page")
    public R<PageResult<SysJobVO>> page(@RequestParam(defaultValue = "1") Integer pageNo,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String jobName,
                                        @RequestParam(required = false) String jobGroup,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(sysJobService.pageVo(pageNo, pageSize, jobName, jobGroup, status));
    }

    @GetMapping("/{id}")
    public R<SysJobDO> detail(@PathVariable Long id) {
        return R.ok(sysJobService.getById(id));
    }

    @PostMapping
    public R<Boolean> create(@RequestBody SysJobDO job) {
        sysJobService.create(job);
        return R.ok(true);
    }

    @PutMapping
    public R<Boolean> update(@RequestBody SysJobDO job) {
        sysJobService.update(job);
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        sysJobService.delete(id);
        return R.ok(true);
    }

    @PutMapping("/changeStatus")
    public R<Boolean> changeStatus(@RequestBody StatusRequest request) {
        sysJobService.changeStatus(request.getId(), request.getStatus());
        return R.ok(true);
    }

    @PostMapping("/run/{id}")
    public R<Boolean> run(@PathVariable Long id) {
        sysJobService.run(id);
        return R.ok(true);
    }

    // ===== 调度日志 =====

    @GetMapping("/log/statistics")
    public R<Map<String, Object>> logStatistics() {
        return R.ok(sysJobLogService.statistics());
    }

    @GetMapping("/log/page")
    public R<PageResult<SysJobLogDO>> logPage(@RequestParam(defaultValue = "1") Integer pageNo,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String jobName,
                                              @RequestParam(required = false) String jobGroup,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(sysJobLogService.page(pageNo, pageSize, jobName, jobGroup, status));
    }

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

    @GetMapping("/log/recycle/page")
    public R<PageResult<SysJobLogDO>> logRecyclePage(@RequestParam(defaultValue = "1") Integer pageNo,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String jobName,
                                                     @RequestParam(required = false) String jobGroup) {
        return R.ok(sysJobLogService.recyclePage(pageNo, pageSize, jobName, jobGroup));
    }

    @PutMapping("/log/restore")
    public R<Boolean> restoreLog(@RequestParam Long id) {
        sysJobLogService.restore(id);
        return R.ok(true);
    }

    @DeleteMapping("/log/delete-permanent")
    public R<Boolean> deleteLogPermanent(@RequestParam Long id) {
        sysJobLogService.deletePermanent(id);
        return R.ok(true);
    }

    @GetMapping("/recycle/page")
    public R<PageResult<SysJobDO>> recyclePage(@RequestParam(defaultValue = "1") Integer pageNo,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String jobName,
                                               @RequestParam(required = false) String jobGroup) {
        return R.ok(sysJobService.recyclePage(pageNo, pageSize, jobName, jobGroup));
    }

    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam Long id) {
        sysJobService.restore(id);
        return R.ok(true);
    }

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
