package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 最近登录记录（工作台展示用）
 */
@Data
public class RecentLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    /** 用户昵称，由系统服务回填 */
    private String nickname;

    /** 用户头像，由系统服务回填 */
    private String avatar;

    private String ipaddr;

    private String loginLocation;

    private String browser;

    private String os;

    /** 登录状态：0-成功 1-失败 */
    private Integer status;

    private LocalDateTime loginTime;
}
