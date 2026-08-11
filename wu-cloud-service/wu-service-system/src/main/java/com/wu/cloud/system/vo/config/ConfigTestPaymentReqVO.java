package com.wu.cloud.system.vo.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付配置连通性测试请求
 */
@Data
public class ConfigTestPaymentReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付类型：wechat / alipay */
    private String type;
}
