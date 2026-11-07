package com.wu.cloud.infra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.EmailLogDO;
import com.wu.cloud.infra.entity.SmsLogDO;
import com.wu.cloud.infra.mapper.EmailLogMapper;
import com.wu.cloud.infra.mapper.SmsLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 短信/邮件发送记录查询与清理 REST 控制器。
 * <p>sys_sms_log / sys_email_log 属主为本服务；配置页的日志分页、最近记录、
 * 删除/批量删除/清空均由此提供。</p>
 */
@Tag(name = "配置变更日志")
@RestController
@RequestMapping("/api/infra/config-log")
public class ConfigLogController {

    @Resource
    private SmsLogMapper smsLogMapper;
    @Resource
    private EmailLogMapper emailLogMapper;

    // ===== 短信日志 =====

    @Operation(summary = "最近短信发送记录")
    @GetMapping("/sms-logs/recent")
    public R<List<SmsLogDO>> recentSmsLogs(@RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        List<SmsLogDO> list = smsLogMapper.selectList(
                new LambdaQueryWrapper<SmsLogDO>().orderByDesc(SmsLogDO::getCreateTime).last("LIMIT " + size));
        return R.ok(list);
    }

    @Operation(summary = "分页查询短信发送记录")
    @GetMapping("/sms-logs")
    public R<PageResult<SmsLogDO>> smsLogs(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        List<SmsLogDO> list = smsLogMapper.selectPage(phone, status,
                (long) (pageNo - 1) * pageSize, pageSize);
        long total = smsLogMapper.selectCount(phone, status);
        return R.ok(PageResult.of(list, total));
    }

    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/sms-logs/{id}")
    public R<Boolean> deleteSmsLog(@PathVariable("id") Long id) {
        return R.ok(smsLogMapper.deleteById(id) > 0);
    }

    @Operation(summary = "批量删除短信发送记录")
    @DeleteMapping("/sms-logs/batch")
    public R<Boolean> deleteSmsLogs(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.ok(false);
        }
        smsLogMapper.deleteByIds(ids);
        return R.ok(true);
    }

    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/sms-logs/clean")
    public R<Boolean> cleanSmsLogs() {
        smsLogMapper.delete(null);
        return R.ok(true);
    }

    // ===== 邮件日志 =====

    @Operation(summary = "最近短信发送记录")
    @GetMapping("/email-logs/recent")
    public R<List<EmailLogDO>> recentEmailLogs(@RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        List<EmailLogDO> list = emailLogMapper.selectList(
                new LambdaQueryWrapper<EmailLogDO>().orderByDesc(EmailLogDO::getCreateTime).last("LIMIT " + size));
        return R.ok(list);
    }

    @Operation(summary = "分页查询邮件发送记录")
    @GetMapping("/email-logs")
    public R<PageResult<EmailLogDO>> emailLogs(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String email,
                                               @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        List<EmailLogDO> list = emailLogMapper.selectPage(email, status,
                (long) (pageNo - 1) * pageSize, pageSize);
        long total = emailLogMapper.selectCount(email, status);
        return R.ok(PageResult.of(list, total));
    }

    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/email-logs/{id}")
    public R<Boolean> deleteEmailLog(@PathVariable("id") Long id) {
        return R.ok(emailLogMapper.deleteById(id) > 0);
    }

    @Operation(summary = "批量删除短信发送记录")
    @DeleteMapping("/email-logs/batch")
    public R<Boolean> deleteEmailLogs(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.ok(false);
        }
        emailLogMapper.deleteByIds(ids);
        return R.ok(true);
    }

    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/email-logs/clean")
    public R<Boolean> cleanEmailLogs() {
        emailLogMapper.delete(null);
        return R.ok(true);
    }
}
