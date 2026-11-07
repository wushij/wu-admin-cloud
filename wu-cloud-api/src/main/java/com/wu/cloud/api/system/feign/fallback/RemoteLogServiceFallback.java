package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.api.system.feign.RemoteLogService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 远程日志服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteLogServiceFallback implements FallbackFactory<RemoteLogService> {

    @Override
    public RemoteLogService create(Throwable cause) {
        log.error("[Feign降级] wu-service-system 操作日志服务调用失败", cause);
        return new RemoteLogService() {
            @Override
            public R<Boolean> saveOperLog(OperLogDTO operLogDTO) {
                return R.fail("日志服务暂不可用，操作日志记录失败");
            }

            @Override
            public R<Integer> purgeExpiredOperLogs(Integer retentionDays) {
                return R.fail("日志服务暂不可用，操作日志清理失败");
            }

            @Override
            public R<List<OperLogDTO>> listRecentOperLogs(Integer limit) {
                return R.fail("日志服务暂不可用，操作日志导出失败");
            }
        };
    }
}
