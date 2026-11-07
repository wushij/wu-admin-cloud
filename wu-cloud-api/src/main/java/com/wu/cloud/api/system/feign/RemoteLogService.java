package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteLogServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 远程操作日志服务 OpenFeign 契约。
 * <p>操作日志唯一属主为 wu-service-system（sys_oper_log 表），
 * 与管理端「操作日志」查询同源。</p>
 */
@FeignClient(contextId = "remoteLogService", value = "wu-service-system", path = "/inner/log", fallbackFactory = RemoteLogServiceFallback.class)
public interface RemoteLogService {

    @PostMapping("/saveOperLog")
    R<Boolean> saveOperLog(@RequestBody OperLogDTO operLogDTO);

    /**
     * 清理超保留期的操作日志，返回清理条数（由日志属主在自己的库内执行）
     */
    @PostMapping("/oper/purge")
    R<Integer> purgeExpiredOperLogs(@RequestParam("retentionDays") Integer retentionDays);

    /**
     * 最近操作日志（导出场景），按操作时间倒序
     */
    @GetMapping("/oper/list-recent")
    R<List<OperLogDTO>> listRecentOperLogs(@RequestParam(value = "limit", defaultValue = "1000") Integer limit);
}
