package com.wu.cloud.api.infra.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 在线用户展示 VO（对齐单体 OnlineUserVO / 前端 OnlineUser）
 */
@Data
public class OnlineUserVO implements Serializable {

    private Long userId;
    private String loginName;
    private String nickname;
    private String avatar;
    private String deptName;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status;
    private String loginTime;
    private String lastAccessTime;
    private String tokenId;
    private String tokenValue;
}
