package com.wu.cloud.ai.providers;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.ai.entity.AiModelDO;
import com.wu.cloud.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 大模型接入协议策略工厂
 * <p>按模型配置的 protocol 字段分发，而不是按供应商标识分发，
 * 因此任意厂商（包括自建网关）只要协议一致即可接入。</p>
 */
@Component
public class AiProviderFactory {

    @Resource
    private List<AiProviderStrategy> strategies;

    /** 按协议获取策略实现，不支持时抛业务异常 */
    public AiProviderStrategy getStrategy(AiModelDO model) {
        return getStrategy(model == null ? null : model.getProtocol());
    }

    /** 按协议标识获取策略实现，为空时回退 openai */
    public AiProviderStrategy getStrategy(String protocol) {
        String target = StrUtil.blankToDefault(StrUtil.trim(protocol), "openai").toLowerCase();
        for (AiProviderStrategy strategy : strategies) {
            if (target.equalsIgnoreCase(strategy.protocol())) {
                return strategy;
            }
        }
        throw new BusinessException("不支持的接入协议: " + protocol + "（仅支持 openai/anthropic）");
    }
}
