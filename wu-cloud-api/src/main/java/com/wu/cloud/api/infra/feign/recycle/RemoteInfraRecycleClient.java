package com.wu.cloud.api.infra.feign.recycle;

import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * wu-service-infra 回收站计数（job / jobLog）
 */
@FeignClient(contextId = "recycleInfraClient", value = "wu-service-infra", path = "/inner/recycle")
public interface RemoteInfraRecycleClient extends RecycleCountEndpoint {

    @GetMapping("/count")
    R<Map<String, Object>> count();
}
