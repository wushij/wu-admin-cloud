package com.wu.cloud.api.infra.feign;

import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.api.infra.feign.fallback.RemoteOnlineUserServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 在线用户内部 RPC（会话登记、活跃心跳、人数统计）
 */
@FeignClient(contextId = "remoteOnlineUserService", value = "wu-service-infra",
        path = "/inner/online", fallbackFactory = RemoteOnlineUserServiceFallback.class)
public interface RemoteOnlineUserService {

    @PostMapping("/record-login")
    R<Boolean> recordLoginSession(@RequestBody RecordLoginSessionDTO dto);

    @PostMapping("/touch")
    R<Boolean> touchLastAccess(@RequestParam("userId") Long userId);

    @GetMapping("/count")
    R<Integer> countOnlineUsers();
}
