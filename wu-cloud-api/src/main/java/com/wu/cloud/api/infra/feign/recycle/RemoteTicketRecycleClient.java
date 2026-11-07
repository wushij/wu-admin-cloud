package com.wu.cloud.api.infra.feign.recycle;

import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * wu-service-ticket 回收站计数（ticket / approval / notice）
 */
@FeignClient(contextId = "recycleTicketClient", value = "wu-service-ticket", path = "/inner/recycle")
public interface RemoteTicketRecycleClient extends RecycleCountEndpoint {

    @GetMapping("/count")
    R<Map<String, Object>> count();
}
