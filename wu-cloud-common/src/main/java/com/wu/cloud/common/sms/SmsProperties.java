package com.wu.cloud.common.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信渠道配置
 */
@Data
@ConfigurationProperties(prefix = "wu.sms")
public class SmsProperties {

    /** 是否启用真实发送，false 时使用 Mock 渠道仅打印日志 */
    private boolean enabled = false;

    /** 渠道标识：aliyun / tencent / mock */
    private String channel = "mock";

    /** 访问密钥 ID */
    private String accessKeyId;

    /** 访问密钥 Secret */
    private String accessKeySecret;

    /** 短信签名 */
    private String signName;

    /** 腾讯云 SDK AppId */
    private String sdkAppId;

    /** 服务地域 */
    private String region = "ap-guangzhou";

    /** 验证码模板编号 */
    private String verifyCodeTemplateId;
}
