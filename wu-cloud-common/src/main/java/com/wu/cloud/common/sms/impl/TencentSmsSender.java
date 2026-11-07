package com.wu.cloud.common.sms.impl;

import cn.hutool.core.util.StrUtil;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import com.wu.cloud.common.sms.SmsProperties;
import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 腾讯云短信渠道（SMS 2021-01-11）。
 * <p>模板参数按 Map 的迭代顺序展开为 TemplateParamSet，调用方需保证参数顺序与模板一致。</p>
 */
@Slf4j
public class TencentSmsSender implements SmsSender {

    private static final String CHANNEL = "tencent";
    private static final String ENDPOINT = "sms.tencentcloudapi.com";

    private final SmsProperties properties;

    public TencentSmsSender(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public SmsResult send(String mobile, String templateId, Map<String, String> params) {
        if (StrUtil.isBlank(properties.getAccessKeyId()) || StrUtil.isBlank(properties.getAccessKeySecret())) {
            return SmsResult.fail("SEND_FAILED", "腾讯云短信未配置 SecretId/SecretKey");
        }
        if (StrUtil.isBlank(properties.getSdkAppId())) {
            return SmsResult.fail("SEND_FAILED", "腾讯云短信未配置 SdkAppId");
        }
        if (StrUtil.isBlank(properties.getSignName())) {
            return SmsResult.fail("SEND_FAILED", "腾讯云短信未配置签名");
        }
        String template = StrUtil.blankToDefault(templateId, properties.getVerifyCodeTemplateId());
        if (StrUtil.isBlank(template)) {
            return SmsResult.fail("SEND_FAILED", "腾讯云短信未配置模板编号");
        }
        try {
            Credential credential = new Credential(properties.getAccessKeyId(), properties.getAccessKeySecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(ENDPOINT);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(credential, properties.getRegion(), clientProfile);

            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(properties.getSdkAppId());
            request.setSignName(properties.getSignName());
            request.setTemplateId(template);
            request.setPhoneNumberSet(new String[] { "+86" + mobile });
            if (params != null && !params.isEmpty()) {
                request.setTemplateParamSet(params.values().toArray(new String[0]));
            }
            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statuses = response == null ? null : response.getSendStatusSet();
            if (statuses != null && statuses.length > 0 && "Ok".equalsIgnoreCase(statuses[0].getCode())) {
                return SmsResult.success(response.getRequestId());
            }
            String message = statuses == null || statuses.length == 0
                    ? "腾讯云短信响应为空" : statuses[0].getMessage();
            log.warn("腾讯云短信发送失败 mobile={} message={}", desensitize(mobile), message);
            return SmsResult.fail("SEND_FAILED", message);
        } catch (Exception e) {
            log.error("腾讯云短信发送异常 mobile={} err={}", desensitize(mobile), e.getMessage());
            return SmsResult.fail("SEND_ERROR", "腾讯云短信发送异常: " + e.getMessage());
        }
    }

    private String desensitize(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
