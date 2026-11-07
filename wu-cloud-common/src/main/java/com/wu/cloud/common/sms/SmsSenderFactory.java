package com.wu.cloud.common.sms;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.sms.impl.AliyunSmsSender;
import com.wu.cloud.common.sms.impl.MockSmsSender;
import com.wu.cloud.common.sms.impl.TencentSmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 短信渠道工厂（与单体新版 SmsServiceFactory 对齐）。
 *
 * <p>渠道选择顺序：
 * <ol>
 *   <li>系统配置 sms 分组（运维在配置页动态修改，优先级最高）；</li>
 *   <li>本地 {@code wu.sms.*} 配置（application.yml / 环境变量）；</li>
 *   <li>以上均未配置真实渠道时回退 Mock，只打印日志不产生费用。</li>
 * </ol>
 * 每次发送都按当前配置解析，配置改动无需重启服务。</p>
 */
@Slf4j
public class SmsSenderFactory {

    private static final String GROUP_SMS = "sms";

    private final SmsProperties properties;
    private final ObjectProvider<DynamicConfigHelper> dynamicConfigHelperProvider;
    private final SmsSender mockSender = new MockSmsSender();

    public SmsSenderFactory(SmsProperties properties,
                            ObjectProvider<DynamicConfigHelper> dynamicConfigHelperProvider) {
        this.properties = properties;
        this.dynamicConfigHelperProvider = dynamicConfigHelperProvider;
    }

    /** 当前生效的短信渠道 */
    public SmsSender sender() {
        SmsProperties effective = resolve();
        String channel = StrUtil.nullToEmpty(effective.getChannel()).toLowerCase();
        if (!effective.isEnabled()) {
            return mockSender;
        }
        return switch (channel) {
            case "aliyun", "aliyunAuth" -> new AliyunSmsSender(effective);
            case "tencent" -> new TencentSmsSender(effective);
            default -> mockSender;
        };
    }

    /** 当前渠道名，供配置页展示与日志诊断 */
    public String currentChannel() {
        SmsProperties effective = resolve();
        return effective.isEnabled()
                ? StrUtil.nullToEmpty(effective.getChannel()).toLowerCase()
                : "mock";
    }

    /**
     * 合并系统配置与本地配置：系统配置项非空时覆盖本地项。
     * <p>配置读取失败时直接使用本地配置，避免基础设施故障阻断短信链路。</p>
     */
    private SmsProperties resolve() {
        SmsProperties effective = copyOf(properties);
        DynamicConfigHelper helper = dynamicConfigHelperProvider.getIfAvailable();
        if (helper == null) {
            return effective;
        }
        try {
            String provider = helper.getStr(GROUP_SMS, "provider", "");
            if (StrUtil.isNotBlank(provider)) {
                effective.setChannel("aliyun".equalsIgnoreCase(provider) ? "aliyunAuth" : provider.trim());
            }
            Boolean enabled = helper.getGroup(GROUP_SMS).containsKey("enabled")
                    ? helper.isSmsEnabled() : null;
            if (enabled != null) {
                effective.setEnabled(enabled);
            }
            String accessKeyId = helper.getStr(GROUP_SMS, "accessKeyId", "");
            if (StrUtil.isNotBlank(accessKeyId)) {
                effective.setAccessKeyId(accessKeyId.trim());
            }
            String accessKeySecret = helper.getStr(GROUP_SMS, "accessKeySecret", "");
            if (StrUtil.isNotBlank(accessKeySecret)) {
                effective.setAccessKeySecret(accessKeySecret.trim());
            }
            String signName = helper.getStr(GROUP_SMS, "signName", "");
            if (StrUtil.isNotBlank(signName)) {
                effective.setSignName(signName.trim());
            }
            String sdkAppId = helper.getStr(GROUP_SMS, "sdkAppId", "");
            if (StrUtil.isNotBlank(sdkAppId)) {
                effective.setSdkAppId(sdkAppId.trim());
            }
            String region = helper.getStr(GROUP_SMS, "region", "");
            if (StrUtil.isNotBlank(region)) {
                effective.setRegion(region.trim());
            }
            String templateCode = helper.getStr(GROUP_SMS, "templateCode", "");
            if (StrUtil.isBlank(templateCode)) {
                templateCode = helper.getStr(GROUP_SMS, "verifyCodeTemplateId", "");
            }
            if (StrUtil.isNotBlank(templateCode)) {
                effective.setVerifyCodeTemplateId(templateCode.trim());
            }
        } catch (Exception e) {
            log.warn("读取系统短信配置失败，使用本地配置: {}", e.getMessage());
        }
        return effective;
    }

    private SmsProperties copyOf(SmsProperties source) {
        SmsProperties copy = new SmsProperties();
        if (source == null) {
            return copy;
        }
        copy.setEnabled(source.isEnabled());
        copy.setChannel(source.getChannel());
        copy.setAccessKeyId(source.getAccessKeyId());
        copy.setAccessKeySecret(source.getAccessKeySecret());
        copy.setSignName(source.getSignName());
        copy.setSdkAppId(source.getSdkAppId());
        copy.setRegion(source.getRegion());
        copy.setVerifyCodeTemplateId(source.getVerifyCodeTemplateId());
        return copy;
    }
}
