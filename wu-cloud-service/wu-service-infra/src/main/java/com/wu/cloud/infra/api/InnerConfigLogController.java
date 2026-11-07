package com.wu.cloud.infra.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.infra.dto.EmailLogDTO;
import com.wu.cloud.api.infra.dto.SmsLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.EmailLogDO;
import com.wu.cloud.infra.entity.SmsLogDO;
import com.wu.cloud.infra.mapper.EmailLogMapper;
import com.wu.cloud.infra.mapper.SmsLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 短信/邮件发送记录上报内部端点。
 * <p>sys_sms_log / sys_email_log 属主为本服务，auth / system 等发送方
 * 通过 OpenFeign 上报每次发送的目标、渠道、结果与耗时上下文。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/config-log")
public class InnerConfigLogController {

    @Resource
    private SmsLogMapper smsLogMapper;
    @Resource
    private EmailLogMapper emailLogMapper;

    @Operation(summary = "保存")
    @PostMapping("/sms")
    public R<Boolean> saveSmsLog(@RequestBody SmsLogDTO dto) {
        if (dto == null || dto.getPhone() == null || dto.getPhone().isBlank()) {
            return R.ok(false);
        }
        SmsLogDO logDO = new SmsLogDO();
        logDO.setPhone(dto.getPhone());
        logDO.setContent(dto.getContent());
        logDO.setSmsType(dto.getSmsType() == null ? "verify_code" : dto.getSmsType());
        logDO.setTemplateId(dto.getTemplateId());
        logDO.setTemplateParams(dto.getTemplateParams());
        logDO.setProvider(dto.getProvider());
        logDO.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        logDO.setResultMsg(dto.getResultMsg());
        logDO.setBizId(dto.getBizId());
        logDO.setUserId(dto.getUserId());
        logDO.setBizType(dto.getBizType());
        logDO.setIp(dto.getIp());
        logDO.setSendTime(LocalDateTime.now());
        logDO.setCreateTime(LocalDateTime.now());
        smsLogMapper.insert(logDO);
        return R.ok(true);
    }

    @Operation(summary = "保存")
    @PostMapping("/email")
    public R<Boolean> saveEmailLog(@RequestBody EmailLogDTO dto) {
        if (dto == null || dto.getEmail() == null || dto.getEmail().isBlank()) {
            return R.ok(false);
        }
        EmailLogDO logDO = new EmailLogDO();
        logDO.setEmail(dto.getEmail());
        logDO.setSubject(dto.getSubject());
        logDO.setContent(dto.getContent());
        logDO.setScene(dto.getScene() == null ? "notice" : dto.getScene());
        logDO.setProvider(dto.getProvider());
        logDO.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        logDO.setResultMsg(dto.getResultMsg());
        logDO.setIp(dto.getIp());
        logDO.setCreateTime(LocalDateTime.now());
        emailLogMapper.insert(logDO);
        return R.ok(true);
    }
}
