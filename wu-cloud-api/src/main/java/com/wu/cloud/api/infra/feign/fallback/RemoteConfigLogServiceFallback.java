package com.wu.cloud.api.infra.feign.fallback;

import com.wu.cloud.api.infra.dto.EmailLogDTO;
import com.wu.cloud.api.infra.dto.SmsLogDTO;
import com.wu.cloud.api.infra.feign.RemoteConfigLogService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 短信/邮件发送记录上报 Feign 降级工厂
 * <p>日志上报属于旁路动作，降级仅记录告警，不影响发送主流程。</p>
 */
@Slf4j
@Component
public class RemoteConfigLogServiceFallback implements FallbackFactory<RemoteConfigLogService> {

    @Override
    public RemoteConfigLogService create(Throwable cause) {
        log.warn("[Feign降级] wu-service-infra 短信/邮件日志上报失败: {}", cause.getMessage());
        return new RemoteConfigLogService() {
            @Override
            public R<Boolean> saveSmsLog(SmsLogDTO dto) {
                return R.fail("日志服务暂不可用");
            }

            @Override
            public R<Boolean> saveEmailLog(EmailLogDTO dto) {
                return R.fail("日志服务暂不可用");
            }
        };
    }
}
