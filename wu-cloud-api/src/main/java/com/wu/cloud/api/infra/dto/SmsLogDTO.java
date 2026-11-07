package com.wu.cloud.api.infra.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信发送记录上报 DTO（写入 infra 域 sys_sms_log）
 */
@Data
public class SmsLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String phone;
    private String content;
    /** verify_code / notice / test */
    private String smsType;
    private String templateId;
    private String templateParams;
    private String provider;
    /** 1-成功 2-失败 */
    private Integer status;
    private String resultMsg;
    private String bizId;
    private Long userId;
    private String bizType;
    private String ip;
}
