package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.feign.fallback.RemoteConfigServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 远程配置服务：auth 通过此接口从 system 读 sys_config_group
 */
@FeignClient(contextId = "remoteConfigService", value = "wu-service-system", path = "/inner/system/config",
        fallbackFactory = RemoteConfigServiceFallback.class)
public interface RemoteConfigService {

    @GetMapping("/get-group")
    R<Map<String, Object>> getGroup(@RequestParam("groupCode") String groupCode);
}
