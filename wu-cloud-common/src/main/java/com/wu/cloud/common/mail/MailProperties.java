package com.wu.cloud.common.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件渠道配置
 */
@Data
@ConfigurationProperties(prefix = "wu.mail")
public class MailProperties {

    /** 是否启用真实发送，false 时使用 Mock 渠道仅打印日志 */
    private boolean enabled = false;

    /** SMTP 服务器地址 */
    private String host;

    /** SMTP 端口 */
    private Integer port = 465;

    /** 发件账号 */
    private String username;

    /** 发件密码或授权码 */
    private String password;

    /** 发件人显示名称 */
    private String fromName = "系统通知";

    /** 是否开启 SSL */
    private boolean sslEnable = true;
}
