package com.wu.cloud.api.infra.feign.recycle;

import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * wu-service-message 回收站计数（announce）
 */
@FeignClient(contextId = "recycleMessageClient", value = "wu-service-message", path = "/inner/recycle")
public interface RemoteMessageRecycleClient extends RecycleCountEndpoint {

    @GetMapping("/count")
    R<Map<String, Object>> count();
}
