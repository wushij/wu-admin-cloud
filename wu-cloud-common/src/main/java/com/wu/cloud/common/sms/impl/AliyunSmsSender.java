package com.wu.cloud.common.sms.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.wu.cloud.common.sms.SmsProperties;
import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 阿里云短信渠道（dysmsapi 2017-05-25）。
 * <p>签名、模板与密钥由 {@link SmsProperties} 或系统配置 sms 分组提供；
 * 发送成功以响应体 code=OK 为准，失败时保留服务端 requestId 便于排查。</p>
 */
@Slf4j
public class AliyunSmsSender implements SmsSender {

    private static final String CHANNEL = "aliyun";
    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

    private final SmsProperties properties;

    public AliyunSmsSender(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public SmsResult send(String mobile, String templateId, Map<String, String> params) {
        if (StrUtil.isBlank(properties.getAccessKeyId()) || StrUtil.isBlank(properties.getAccessKeySecret())) {
            return SmsResult.fail("SEND_FAILED", "阿里云短信未配置 AccessKey");
        }
        if (StrUtil.isBlank(properties.getSignName())) {
            return SmsResult.fail("SEND_FAILED", "阿里云短信未配置签名");
        }
        String template = StrUtil.blankToDefault(templateId, properties.getVerifyCodeTemplateId());
        if (StrUtil.isBlank(template)) {
            return SmsResult.fail("SEND_FAILED", "阿里云短信未配置模板编号");
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret());
            config.endpoint = ENDPOINT;
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(template)
                    .setTemplateParam(params == null || params.isEmpty() ? null : JSONUtil.toJsonStr(params));
            SendSmsResponse response = client.sendSms(request);
            String code = response == null || response.getBody() == null ? null : response.getBody().getCode();
            if ("OK".equalsIgnoreCase(code)) {
                return SmsResult.success(response.getBody().getRequestId());
            }
            String message = response == null || response.getBody() == null
                    ? "阿里云短信响应为空" : response.getBody().getMessage();
            log.warn("阿里云短信发送失败 mobile={} code={} message={}", desensitize(mobile), code, message);
            return SmsResult.fail("SEND_FAILED", message);
        } catch (Exception e) {
            log.error("阿里云短信发送异常 mobile={} err={}", desensitize(mobile), e.getMessage());
            return SmsResult.fail("SEND_ERROR", "阿里云短信发送异常: " + e.getMessage());
        }
    }

    private String desensitize(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
