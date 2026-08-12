package com.wu.cloud.system.controller;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SysOperLogDO;
import com.wu.cloud.system.mapper.OperLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志内部 RPC 端点（实现 RemoteLogService 契约）。
 * <p>写入 {@code sys_oper_log}，与对外查询 {@code /api/system/oper-log}、单体表结构一致。</p>
 */
@RestController
@RequestMapping("/inner/log")
public class InnerLogController {

    @Resource
    private OperLogMapper operLogMapper;

    @PostMapping("/saveOperLog")
    public R<Boolean> saveOperLog(@RequestBody OperLogDTO dto) {
        if (dto == null) {
            return R.ok(false);
        }
        SysOperLogDO logDO = new SysOperLogDO();
        logDO.setTitle(dto.getTitle());
        logDO.setBusinessType(dto.getBusinessType());
        logDO.setMethod(dto.getMethod());
        logDO.setRequestMethod(dto.getRequestMethod());
        logDO.setOperName(dto.getOperName());
        logDO.setOperUrl(dto.getOperUrl());
        logDO.setOperIp(dto.getOperIp());
        logDO.setOperParam(dto.getOperParam());
        logDO.setJsonResult(dto.getJsonResult());
        logDO.setStatus(dto.getStatus());
        logDO.setErrorMsg(dto.getErrorMsg());
        logDO.setCostTime(dto.getCostTime());
        logDO.setOperTime(dto.getOperTime() != null ? dto.getOperTime() : LocalDateTime.now());
        operLogMapper.insert(logDO);
        return R.ok(true);
    }
}
