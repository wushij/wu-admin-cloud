package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 远程登录日志服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteLoginLogServiceFallback implements FallbackFactory<RemoteLoginLogService> {

    @Override
    public RemoteLoginLogService create(Throwable cause) {
        log.error("[Feign降级] wu-service-system 登录日志服务调用失败", cause);
        return new RemoteLoginLogService() {
            @Override
            public R<Boolean> saveLoginLog(LoginLogDTO loginLogDTO) {
                return R.fail("登录日志服务暂不可用");
            }

            @Override
            public R<PageResult<LoginLogDTO>> pageByUsername(String username, Integer pageNo, Integer pageSize) {
                return R.fail("登录日志服务暂不可用");
            }
        };
    }
}
