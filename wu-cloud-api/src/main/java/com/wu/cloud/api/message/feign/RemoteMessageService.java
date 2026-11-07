package com.wu.cloud.api.message.feign;

import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.api.message.feign.fallback.RemoteMessageServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

    /** 最新已发布公告列表（AI 工具 / 动态资讯场景） */
    @GetMapping("/announce/latest")
    R<List<SysAnnounceDTO>> listLatestAnnounces(@RequestParam(value = "limit", defaultValue = "5") Integer limit);

    /** 清理超保留期的私聊消息，返回清理条数 */
    @PostMapping("/purge/chat")
    R<Integer> purgeOldChatMessages(@RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays);

    /** 清理超保留期的群聊消息，返回清理条数 */
    @PostMapping("/purge/group-chat")
    R<Integer> purgeOldGroupChatMessages(@RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays);

    /** 清理超保留期的已读公告阅读记录，返回清理条数 */
    @PostMapping("/purge/read-notice")
    R<Integer> purgeReadNotices(@RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays);
}
