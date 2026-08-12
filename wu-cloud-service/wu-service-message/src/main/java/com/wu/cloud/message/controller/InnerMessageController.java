package com.wu.cloud.message.controller;

import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.message.entity.SysAnnounceDO;
import com.wu.cloud.message.mapper.SysAnnounceMapper;
import com.wu.cloud.message.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息服务内部 RPC 端点。
 *
 * <p>契约对齐：返回值统一使用 {@code SysAnnounceDTO}，与 wu-cloud-api 中
 * {@code RemoteMessageService} 声明一致。</p>
 */
@RestController
@RequestMapping("/inner/message")
public class InnerMessageController {

    @Resource
    private SysAnnounceMapper sysAnnounceMapper;

    @Resource
    private ChatService chatService;

    @GetMapping("/announce/{id}")
    public R<SysAnnounceDTO> getAnnounceById(@PathVariable("id") Long id) {
        return R.ok(toDTO(sysAnnounceMapper.selectById(id)));
    }

    /** 工作台企业 IM 未读数（对齐单体 chatService.unreadCount） */
    @GetMapping("/chat/unread-count/{userId}")
    public R<Long> getChatUnreadCount(@PathVariable("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return R.ok(0L);
        }
        return R.ok(chatService.unreadCount(userId));
    }

    private SysAnnounceDTO toDTO(SysAnnounceDO announce) {
        if (announce == null) {
            return null;
        }
        SysAnnounceDTO dto = new SysAnnounceDTO();
        dto.setId(announce.getId());
        dto.setTitle(announce.getTitle());
        dto.setContent(announce.getContent());
        dto.setPriority(announce.getPriority());
        dto.setTargetType(announce.getTargetType());
        dto.setStatus(announce.getStatus());
        dto.setCreateTime(announce.getCreateTime());
        return dto;
    }
}
