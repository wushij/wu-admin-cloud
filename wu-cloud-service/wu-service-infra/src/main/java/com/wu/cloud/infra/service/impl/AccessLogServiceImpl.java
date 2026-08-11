package com.wu.cloud.infra.service.impl;

import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.mapper.AccessLogMapper;
import com.wu.cloud.infra.service.AccessLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * API 访问日志服务实现
 */
@Service
public class AccessLogServiceImpl implements AccessLogService {

    @Resource
    private AccessLogMapper accessLogMapper;

    @Override
    public void saveAccessLog(AccessLogDO accessLogDO) {
        if (accessLogDO.getAccessTime() == null) {
            accessLogDO.setAccessTime(java.time.LocalDateTime.now());
        }
        accessLogMapper.insert(accessLogDO);
    }
}
