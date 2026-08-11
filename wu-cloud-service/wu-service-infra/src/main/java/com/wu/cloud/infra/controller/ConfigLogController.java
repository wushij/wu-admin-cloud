package com.wu.cloud.infra.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.EmailLogDO;
import com.wu.cloud.infra.entity.SmsLogDO;
import com.wu.cloud.infra.mapper.EmailLogMapper;
import com.wu.cloud.infra.mapper.SmsLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 短信/邮件发送记录查询 REST 控制器。
 * <p>C 队补全：网关 compat-config-log 将前端 /system/config-group/sms-logs|email-logs/**
 * 重写为 /api/infra/config-log/**，此处提供最近记录与分页查询，供配置页"短信/邮件"Tab 展示。</p>
 */
@RestController
@RequestMapping("/api/infra/config-log")
public class ConfigLogController {

    @Resource
    private SmsLogMapper smsLogMapper;
    @Resource
    private EmailLogMapper emailLogMapper;

    // ===== 短信日志 =====

    @GetMapping("/sms-logs/recent")
    public R<List<SmsLogDO>> recentSmsLogs(@RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        List<SmsLogDO> list = smsLogMapper.selectList(
                new LambdaQueryWrapper<SmsLogDO>().orderByDesc(SmsLogDO::getCreateTime).last("LIMIT " + size));
        return R.ok(list);
    }

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

    // ===== 邮件日志 =====

    @GetMapping("/email-logs/recent")
    public R<List<EmailLogDO>> recentEmailLogs(@RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        List<EmailLogDO> list = emailLogMapper.selectList(
                new LambdaQueryWrapper<EmailLogDO>().orderByDesc(EmailLogDO::getCreateTime).last("LIMIT " + size));
        return R.ok(list);
    }

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
}
