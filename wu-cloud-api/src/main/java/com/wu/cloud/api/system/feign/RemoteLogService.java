package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteLogServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程操作日志服务 OpenFeign 契约。
 * <p>写入 system 的 {@code sys_oper_log}，与管理端「操作日志」查询同源（对齐单体）。</p>
 */
@FeignClient(contextId = "remoteLogService", value = "wu-service-system", path = "/inner/log", fallbackFactory = RemoteLogServiceFallback.class)
public interface RemoteLogService {

    @PostMapping("/saveOperLog")
    R<Boolean> saveOperLog(@RequestBody OperLogDTO operLogDTO);
}
