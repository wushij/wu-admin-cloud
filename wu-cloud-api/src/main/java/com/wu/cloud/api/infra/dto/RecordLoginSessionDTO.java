package com.wu.cloud.api.infra.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功后写入在线会话详情（供 infra 在线用户模块使用）
 */
@Data
public class RecordLoginSessionDTO implements Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private String clientIp;
    private String userAgent;
}
