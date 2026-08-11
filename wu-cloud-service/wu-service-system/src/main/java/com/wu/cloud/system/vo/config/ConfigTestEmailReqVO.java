package com.wu.cloud.system.vo.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件配置连通性测试请求
 */
@Data
public class ConfigTestEmailReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收测试邮件的邮箱 */
    private String toEmail;
}
