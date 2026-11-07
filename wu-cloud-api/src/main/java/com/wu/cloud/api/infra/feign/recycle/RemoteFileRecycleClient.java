package com.wu.cloud.api.infra.feign.recycle;

import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * wu-service-file 回收站计数（file / gen）
 */
@FeignClient(contextId = "recycleFileClient", value = "wu-service-file", path = "/inner/recycle")
public interface RemoteFileRecycleClient extends RecycleCountEndpoint {

    @GetMapping("/count")
    R<Map<String, Object>> count();
}
