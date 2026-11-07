package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 风险事件上报 DTO（任意服务 → system 风控属主）
 */
@Data
public class SecurityEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型：PERMISSION_DENIED / UA_BLOCK / ANTI_REPLAY / HONEYPOT / RATE_LIMIT ... */
    private String eventType;

    /** 主体标识：ip:{ip} 或 user:{id} */
    private String principal;

    /** 客户端真实 IP */
    private String ip;

    /** 风险分增量 */
    private Integer delta;

    /** 处置动作：BLOCKED / WARNING / CHALLENGE / NOTICE */
    private String action;

    private String uri;

    private String method;

    private String userAgent;

    private String reason;
}
