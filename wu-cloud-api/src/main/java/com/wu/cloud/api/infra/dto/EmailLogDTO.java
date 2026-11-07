package com.wu.cloud.api.infra.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件发送记录上报 DTO（写入 infra 域 sys_email_log）
 */
@Data
public class EmailLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String email;
    private String subject;
    private String content;
    /** verify_code / notice / test */
    private String scene;
    private String provider;
    /** 1-成功 2-失败 */
    private Integer status;
    private String resultMsg;
    private String ip;
}
