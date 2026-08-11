package com.wu.cloud.pay.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.PageResult;
import com.wu.cloud.pay.entity.PayConfigDO;

/**
 * 支付配置 Service
 */
public interface PayConfigService {

    PageResult<PayConfigDO> page(PageParam pageParam, String payType, String name, Integer status);

    PayConfigDO detail(Long id);

    Long create(PayConfigDO config);

    void update(PayConfigDO config);

    void delete(Long id);

    /**
     * 获取指定支付类型启用的配置（apiKey 解密返回）
     */
    PayConfigDO getEnabledConfig(String payType);
}
