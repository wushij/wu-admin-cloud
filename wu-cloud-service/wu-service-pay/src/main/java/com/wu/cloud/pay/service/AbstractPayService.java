package com.wu.cloud.pay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支付服务抽象基类。
 * <p>具体渠道（微信 / 支付宝）在子类接入官方 SDK；本地默认返回可联调的占位结果。</p>
 */
public abstract class AbstractPayService implements PayService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 生成支付二维码内容（如启用 zxing 可自行实现，这里返回占位内容）
     */
    protected String generateQRCodeContent(String content) {
        return content;
    }
}
