package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteLogServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程日志服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteLogService", value = "wu-service-infra", path = "/inner/log", fallbackFactory = RemoteLogServiceFallback.class)
public interface RemoteLogService {

    @PostMapping("/saveOperLog")
    R<Boolean> saveOperLog(@RequestBody OperLogDTO operLogDTO);
}
