package com.wu.cloud.common.sms;

import java.util.Map;

/**
 * 短信发送统一抽象接口
 *
 * <p>各云厂商（阿里云 / 腾讯云）实现该接口，业务侧仅依赖抽象。</p>
 */
public interface SmsSender {

    /**
     * 渠道标识，如 aliyun / tencent / mock
     *
     * @return 渠道编码
     */
    String channel();

    /**
     * 发送模板短信
     *
     * @param mobile     手机号
     * @param templateId 模板编号
     * @param params     模板参数
     * @return 发送结果
     */
    SmsResult send(String mobile, String templateId, Map<String, String> params);
}
