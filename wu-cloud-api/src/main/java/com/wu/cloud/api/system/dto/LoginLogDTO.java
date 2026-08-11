package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 DTO（跨服务传输）
 */
@Data
public class LoginLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户主键 */
    private Long userId;

    /** 账号 */
    private String username;

    /** 登录 IP */
    private String ip;

    /** 登录地点 */
    private String address;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录状态：0 成功 1 失败 */
    private Integer status;

    /** 提示消息 */
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
