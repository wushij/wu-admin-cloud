package com.wu.cloud.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.utils.crypto.SmCryptoUtils;
import com.wu.cloud.pay.entity.PayConfigDO;
import com.wu.cloud.pay.mapper.PayConfigMapper;
import com.wu.cloud.pay.service.PayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * 支付配置 Service 实现：apiKey 加密存储 / 解密读取
 */
@Slf4j
@Service
public class PayConfigServiceImpl extends ServiceImpl<PayConfigMapper, PayConfigDO> implements PayConfigService {

    /**
     * SM4 加解密密钥与 IV（16 字节）
     * <p>真实环境应从应用配置 / 环境变量 / 密钥管理服务读取，禁止硬编码在生产代码中。
     * 未配置时回退到默认值（仅用于本地开发，正式部署务必通过环境变量覆盖）。</p>
     */
    @Value("${pay.crypto.sm4-key:wucloud_pay_key1}")
    private String sm4Key;
    @Value("${pay.crypto.sm4-iv:wucloud_pay_iv_1}")
    private String sm4Iv;

    private byte[] getKey() {
        String key = sm4Key != null ? sm4Key : "wucloud_pay_key1";
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 16) {
            throw new IllegalStateException("pay.crypto.sm4-key 必须为 16 字节");
        }
        return bytes;
    }

    private byte[] getIv() {
        String iv = sm4Iv != null ? sm4Iv : "wucloud_pay_iv_1";
        byte[] bytes = iv.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 16) {
            throw new IllegalStateException("pay.crypto.sm4-iv 必须为 16 字节");
        }
        return bytes;
    }

    @Override
    public PageResult<PayConfigDO> page(PageParam pageParam, String payType, String name, Integer status) {
        Page<PayConfigDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<PayConfigDO> wrapper = new LambdaQueryWrapper<>();
        if (payType != null && !payType.isBlank()) {
            wrapper.eq(PayConfigDO::getPayType, payType);
        }
        if (name != null && !name.isBlank()) {
            wrapper.like(PayConfigDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(PayConfigDO::getStatus, status);
        }
        wrapper.orderByDesc(PayConfigDO::getCreateTime);
        Page<PayConfigDO> result = page(page, wrapper);
        // 列表不返回明文 apiKey，仅解密便于查看时使用 detail
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public PayConfigDO detail(Long id) {
        PayConfigDO config = getById(id);
        if (config == null) {
            throw new BusinessException(404, "支付配置不存在");
        }
        decryptApiKey(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PayConfigDO config) {
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        encryptApiKey(config);
        save(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PayConfigDO config) {
        PayConfigDO exist = getById(config.getId());
        if (exist == null) {
            throw new BusinessException(404, "支付配置不存在");
        }
        // apiKey 为空时不覆盖原密钥
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            config.setApiKey(exist.getApiKey());
        } else {
            encryptApiKey(config);
        }
        updateById(config);
    }

    @Override
    public void delete(Long id) {
        PayConfigDO exist = getById(id);
        if (exist == null) {
            throw new BusinessException(404, "支付配置不存在");
        }
        removeById(id);
    }

    @Override
    public PayConfigDO getEnabledConfig(String payType) {
        PayConfigDO config = getOne(new LambdaQueryWrapper<PayConfigDO>()
                .eq(PayConfigDO::getPayType, payType)
                .eq(PayConfigDO::getStatus, 1)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BusinessException("未找到启用的支付配置: " + payType);
        }
        decryptApiKey(config);
        return config;
    }

    private void encryptApiKey(PayConfigDO config) {
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(SmCryptoUtils.encryptSm4Cbc(apiKey, getKey(), getIv()));
        }
    }

    private void decryptApiKey(PayConfigDO config) {
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                config.setApiKey(SmCryptoUtils.decryptSm4Cbc(apiKey, getKey(), getIv()));
            } catch (Exception e) {
                log.warn("apiKey 解密失败，可能为历史明文: {}", e.getMessage());
            }
        }
    }
}
