package com.wu.cloud.api.message.feign;

import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.api.message.feign.fallback.RemoteMessageServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 远程消息服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteMessageService", value = "wu-service-message", path = "/inner/message", fallbackFactory = RemoteMessageServiceFallback.class)
public interface RemoteMessageService {

    @GetMapping("/announce/{id}")
    R<SysAnnounceDTO> getAnnounceById(@PathVariable("id") Long id);

    /** 工作台企业 IM 未读数 */
    @GetMapping("/chat/unread-count/{userId}")
    R<Long> getChatUnreadCount(@PathVariable("userId") Long userId);
}
