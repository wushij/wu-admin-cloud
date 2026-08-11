package com.wu.cloud.common.sms.impl;

import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * Mock 短信渠道：仅打印日志，用于开发与测试环境
 */
@Slf4j
public class MockSmsSender implements SmsSender {

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public SmsResult send(String mobile, String templateId, Map<String, String> params) {
        log.info("[Mock短信] 手机号={} 模板={} 参数={}", desensitize(mobile), templateId, params);
        return SmsResult.success(UUID.randomUUID().toString().replace("-", ""));
    }

    private String desensitize(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
