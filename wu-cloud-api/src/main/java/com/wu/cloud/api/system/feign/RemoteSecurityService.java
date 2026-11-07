package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.SecurityEventDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteSecurityServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 远程安全风控服务 OpenFeign 接口契约。
 * <p>风控属主唯一：wu-service-system。任何服务发现异常行为都可经此上报，
 * 由 system 统一计分、落库并执行封禁；封禁标记写 Redis，各服务共用同一份键约定。</p>
 */
@FeignClient(contextId = "remoteSecurityService", value = "wu-service-system", path = "/inner/security",
        fallbackFactory = RemoteSecurityServiceFallback.class)
public interface RemoteSecurityService {

    /**
     * 上报风险事件
     *
     * @return {score: Integer, level: Integer, levelText: String, blocked: Boolean}
     */
    @PostMapping("/event")
    R<Map<String, Object>> reportEvent(@RequestBody SecurityEventDTO dto);

    /**
     * 查询主体是否被封禁
     */
    @GetMapping("/check")
    R<Boolean> isBlocked(@RequestParam("principal") String principal);
}
