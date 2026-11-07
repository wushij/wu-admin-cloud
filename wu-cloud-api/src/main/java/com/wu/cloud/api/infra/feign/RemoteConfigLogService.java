package com.wu.cloud.api.infra.feign;

import com.wu.cloud.api.infra.dto.EmailLogDTO;
import com.wu.cloud.api.infra.dto.SmsLogDTO;
import com.wu.cloud.api.infra.feign.fallback.RemoteConfigLogServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 短信/邮件发送记录上报契约。
 * <p>发送动作发生在 auth / system 等服务，日志表 sys_sms_log / sys_email_log
 * 属主为 wu-service-infra，发送方经此契约异步上报，保证后台日志页有数可查。</p>
 */
@FeignClient(contextId = "remoteConfigLogService", value = "wu-service-infra", path = "/inner/config-log",
        fallbackFactory = RemoteConfigLogServiceFallback.class)
public interface RemoteConfigLogService {

    @PostMapping("/sms")
    R<Boolean> saveSmsLog(@RequestBody SmsLogDTO dto);

    @PostMapping("/email")
    R<Boolean> saveEmailLog(@RequestBody EmailLogDTO dto);
}
