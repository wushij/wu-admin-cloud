package com.wu.cloud.infra.service;

import com.wu.cloud.infra.entity.AccessLogDO;

/**
 * API 访问日志服务接口
 */
public interface AccessLogService {

    void saveAccessLog(AccessLogDO accessLogDO);
}
