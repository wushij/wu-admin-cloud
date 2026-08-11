package com.wu.cloud.system.vo.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信配置连通性测试请求
 */
@Data
public class ConfigTestSmsReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收测试短信的手机号 */
    private String phone;

    /** 指定模板 CODE，为空时使用配置中的默认测试模板 */
    private String templateCode;
}
