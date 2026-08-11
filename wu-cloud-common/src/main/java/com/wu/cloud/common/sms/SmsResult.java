package com.wu.cloud.common.sms;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信发送结果
 */
@Data
public class SmsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否发送成功 */
    private boolean success;

    /** 渠道返回的业务码 */
    private String code;

    /** 渠道返回的描述信息 */
    private String message;

    /** 渠道流水号 */
    private String bizId;

    public static SmsResult success(String bizId) {
        SmsResult result = new SmsResult();
        result.setSuccess(true);
        result.setCode("OK");
        result.setMessage("发送成功");
        result.setBizId(bizId);
        return result;
    }

    public static SmsResult fail(String code, String message) {
        SmsResult result = new SmsResult();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
