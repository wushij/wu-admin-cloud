package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.feign.RemoteConfigService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 远程配置服务 Feign 降级工厂（P1b）
 */
@Slf4j
@Component
public class RemoteConfigServiceFallback implements FallbackFactory<RemoteConfigService> {

    @Override
    public RemoteConfigService create(Throwable cause) {
        log.error("[Feign降级] wu-service-system 配置服务调用失败", cause);
        return new RemoteConfigService() {
            @Override
            public R<Map<String, Object>> getGroup(String groupCode) {
                return R.fail("配置服务暂不可用");
            }
        };
    }
}
