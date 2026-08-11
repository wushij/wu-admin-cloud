package com.wu.cloud.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志响应 VO
 *
 * <p>字段命名与前端 {@code LoginLogVO} 契约保持一致：
 * DO 中的 {@code ip / address / message} 在此分别映射为
 * {@code ipaddr / loginLocation / msg}，避免前端表格取不到值。</p>
 */
@Data
public class LoginLogRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 账号 */
    private String username;

    /** 登录 IP（对应 DO.ip） */
    private String ipaddr;

    /** 登录地点（对应 DO.address） */
    private String loginLocation;

    /** 登录状态：0 成功 1 失败 */
    private Integer status;

    /** 提示消息（对应 DO.message） */
    private String msg;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;
}
