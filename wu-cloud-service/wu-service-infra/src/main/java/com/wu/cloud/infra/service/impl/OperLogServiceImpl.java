package com.wu.cloud.infra.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.infra.entity.OperLogDO;
import com.wu.cloud.infra.mapper.OperLogMapper;
import com.wu.cloud.infra.service.OperLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现
 */
@Service
public class OperLogServiceImpl implements OperLogService {

    @Resource
    private OperLogMapper operLogMapper;

    @Override
    public void saveOperLog(OperLogDTO dto) {
        OperLogDO operLogDO = new OperLogDO();
        operLogDO.setTitle(dto.getTitle());
        operLogDO.setBusinessType(dto.getBusinessType() == null ? null : String.valueOf(dto.getBusinessType()));
        operLogDO.setMethod(dto.getMethod());
        operLogDO.setRequestMethod(dto.getRequestMethod());
        operLogDO.setOperatorType(dto.getOperatorType() == null ? null : String.valueOf(dto.getOperatorType()));
        operLogDO.setOperName(dto.getOperName());
        operLogDO.setOperUrl(dto.getOperUrl());
        operLogDO.setOperIp(dto.getOperIp());
        operLogDO.setOperParam(dto.getOperParam());
        operLogDO.setJsonResult(dto.getJsonResult());
        operLogDO.setStatus(dto.getStatus());
        operLogDO.setErrorMsg(dto.getErrorMsg());
        operLogDO.setCostTime(dto.getCostTime());
        operLogDO.setOperTime(LocalDateTime.now());
        operLogMapper.insert(operLogDO);
    }

    @Override
    public List<OperLogDO> listRecent(Integer limit) {
        int size = limit == null || limit <= 0 ? 20 : limit;
        LambdaQueryWrapper<OperLogDO> wrapper = new LambdaQueryWrapper<OperLogDO>()
                .orderByDesc(OperLogDO::getOperTime)
                .last("LIMIT " + size);
        return operLogMapper.selectList(wrapper).stream().collect(Collectors.toList());
    }
}
