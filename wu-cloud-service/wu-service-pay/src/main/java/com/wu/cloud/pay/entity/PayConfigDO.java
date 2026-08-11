package com.wu.cloud.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付配置（apiKey 加密存储）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_config")
public class PayConfigDO extends BaseEntity {

    /** 支付类型: ALIPAY / WECHAT / BALANCE */
    private String payType;
    /** 配置名称 */
    private String name;
    /** 应用 ID */
    private String appId;
    /** 商户号 */
    private String mchId;
    /** API 密钥（加密存储） */
    private String apiKey;
    /** 证书路径（微信=商户私钥 PEM；支付宝=平台公钥） */
    private String certPath;
    /** 微信 APIv3 商户证书序列号（16 位十六进制，与私钥是不同值，勿用 certPath 替代） */
    private String certSerialNo;
    /** 支付回调地址 */
    private String notifyUrl;
    /** 退款回调地址 */
    private String refundNotifyUrl;
    /** 状态: 1 启用 / 0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
