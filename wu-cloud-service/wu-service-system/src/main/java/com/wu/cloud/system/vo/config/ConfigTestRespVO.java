package com.wu.cloud.system.vo.config;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置连通性测试统一返回结果
 */
@Data
public class ConfigTestRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 测试是否通过 */
    private boolean success;

    /** 实际使用的渠道标识，如 aliyun / smtp / mock */
    private String channel;

    /** 结果描述，失败时为失败原因 */
    private String message;

    /** 耗时（毫秒） */
    private Long costMillis;

    /** 渠道返回的扩展数据，如支付二维码、短信流水号 */
    private Map<String, Object> data = new LinkedHashMap<>();

    public static ConfigTestRespVO success(String channel, String message) {
        ConfigTestRespVO vo = new ConfigTestRespVO();
        vo.setSuccess(true);
        vo.setChannel(channel);
        vo.setMessage(message);
        return vo;
    }

    public static ConfigTestRespVO fail(String channel, String message) {
        ConfigTestRespVO vo = new ConfigTestRespVO();
        vo.setSuccess(false);
        vo.setChannel(channel);
        vo.setMessage(message);
        return vo;
    }

    /**
     * 追加扩展数据，便于链式调用
     */
    public ConfigTestRespVO put(String key, Object value) {
        if (value != null) {
            this.data.put(key, value);
        }
        return this;
    }
}
