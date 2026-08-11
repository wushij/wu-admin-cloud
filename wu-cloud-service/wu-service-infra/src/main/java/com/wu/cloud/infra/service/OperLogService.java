package com.wu.cloud.infra.service;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.infra.entity.OperLogDO;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperLogService {

    void saveOperLog(OperLogDTO dto);

    List<OperLogDO> listRecent(Integer limit);
}
