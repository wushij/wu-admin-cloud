package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 DO 实体
 *
 * <p>字段与 sys_login_log 表结构保持一致。</p>
 */
@Data
@TableName("sys_login_log")
public class SysLoginLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户主键 */
    private Long userId;

    /** 账号 */
    private String username;

    /** 登录 IP */
    @TableField("ipaddr")
    private String ip;

    /** 登录地点 */
    @TableField("login_location")
    private String address;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录状态：0 成功 1 失败 */
    private Integer status;

    /** 提示消息 */
    @TableField("msg")
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
