package com.wu.cloud.api.infra.feign;

import com.wu.cloud.api.infra.feign.fallback.RemoteJobServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 远程定时任务统计
 */
@FeignClient(contextId = "remoteJobService", value = "wu-service-infra", path = "/inner/job",
        fallbackFactory = RemoteJobServiceFallback.class)
public interface RemoteJobService {

    @GetMapping("/overview")
    R<Map<String, Object>> overview();
}
