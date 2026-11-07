package com.wu.cloud.api.system.feign;

import com.wu.cloud.common.config.ConfigGroupLoader;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 配置组 Feign 回源加载器：Redis 缓存未命中时，通过内网接口向配置属主
 * wu-service-system 拉取 sys_config_group 的 configValue。
 * <p>与 Redis 缓存加载器组成 SPI 链，本加载器排在缓存之后（Order=200）。</p>
 */
@Slf4j
@Component
@Order(200)
public class RemoteConfigGroupLoader implements ConfigGroupLoader {

    private final RemoteConfigService remoteConfigService;

    public RemoteConfigGroupLoader(RemoteConfigService remoteConfigService) {
        this.remoteConfigService = remoteConfigService;
    }

    @Override
    public String load(String groupCode) {
        R<Map<String, Object>> resp = remoteConfigService.getGroup(groupCode);
        if (resp == null || resp.getData() == null) {
            return null;
        }
        Object configValue = resp.getData().get("configValue");
        return configValue == null ? null : String.valueOf(configValue);
    }
}
