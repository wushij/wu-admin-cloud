package com.wu.cloud.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SysOperLogDO;
import com.wu.cloud.system.mapper.OperLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志内部 RPC 端点（实现 RemoteLogService 契约）。
 * <p>操作日志唯一属主为 wu-service-system，读写 {@code sys_oper_log}，
 * 与对外查询 {@code /api/system/oper-log} 同源。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/log")
public class InnerLogController {

    @Resource
    private OperLogMapper operLogMapper;

    @Operation(summary = "保存")
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

    /**
     * 清理超保留期的操作日志（供各服务的定时清理任务远程触发），返回清理条数
     */
    @Operation(summary = "清理")
    @PostMapping("/oper/purge")
    public R<Integer> purgeExpiredOperLogs(@RequestParam("retentionDays") Integer retentionDays) {
        int days = retentionDays == null || retentionDays <= 0 ? 180 : retentionDays;
        int deleted = operLogMapper.delete(new LambdaQueryWrapper<SysOperLogDO>()
                .lt(SysOperLogDO::getOperTime, LocalDateTime.now().minusDays(days)));
        return R.ok(deleted);
    }

    /**
     * 最近操作日志（导出场景），按操作时间倒序
     */
    @Operation(summary = "获取列表")
    @GetMapping("/oper/list-recent")
    public R<List<OperLogDTO>> listRecentOperLogs(
            @RequestParam(value = "limit", defaultValue = "1000") Integer limit) {
        int size = limit == null || limit <= 0 || limit > 5000 ? 1000 : limit;
        List<SysOperLogDO> logs = operLogMapper.selectList(new LambdaQueryWrapper<SysOperLogDO>()
                .orderByDesc(SysOperLogDO::getOperTime)
                .last("LIMIT " + size));
        return R.ok(logs.stream().map(this::convertToDTO).toList());
    }

    private OperLogDTO convertToDTO(SysOperLogDO log) {
        OperLogDTO dto = new OperLogDTO();
        dto.setId(log.getId());
        dto.setTitle(log.getTitle());
        dto.setBusinessType(log.getBusinessType());
        dto.setMethod(log.getMethod());
        dto.setRequestMethod(log.getRequestMethod());
        dto.setOperName(log.getOperName());
        dto.setOperUrl(log.getOperUrl());
        dto.setOperIp(log.getOperIp());
        dto.setStatus(log.getStatus());
        dto.setErrorMsg(log.getErrorMsg());
        dto.setCostTime(log.getCostTime());
        dto.setOperTime(log.getOperTime());
        return dto;
    }
}
