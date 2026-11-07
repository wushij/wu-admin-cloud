package com.wu.cloud.message.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.message.entity.ChatGroupMessageDO;
import com.wu.cloud.message.entity.ChatMessageDO;
import com.wu.cloud.message.entity.SysAnnounceDO;
import com.wu.cloud.message.entity.UserAnnounceDO;
import com.wu.cloud.message.mapper.ChatGroupMessageMapper;
import com.wu.cloud.message.mapper.ChatMessageMapper;
import com.wu.cloud.message.mapper.SysAnnounceMapper;
import com.wu.cloud.message.mapper.UserAnnounceMapper;
import com.wu.cloud.message.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息服务内部 RPC 端点。
 *
 * <p>契约对齐：返回值统一使用 {@code SysAnnounceDTO}，与 wu-cloud-api 中
 * {@code RemoteMessageService} 声明一致。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/message")
public class InnerMessageController {

    @Resource
    private SysAnnounceMapper sysAnnounceMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private ChatGroupMessageMapper chatGroupMessageMapper;
    @Resource
    private UserAnnounceMapper userAnnounceMapper;

    @Resource
    private ChatService chatService;

    @Operation(summary = "下载工单附件")
    @GetMapping("/announce/{id}")
    public R<SysAnnounceDTO> getAnnounceById(@PathVariable("id") Long id) {
        return R.ok(toDTO(sysAnnounceMapper.selectById(id)));
    }

    /** 工作台企业 IM 未读数 */
    @Operation(summary = "获取统计信息")
    @GetMapping("/chat/unread-count/{userId}")
    public R<Long> getChatUnreadCount(@PathVariable("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return R.ok(0L);
        }
        return R.ok(chatService.unreadCount(userId));
    }

    /** 最新已发布公告列表（最多 5 条，按发布时间倒序） */
    @Operation(summary = "获取列表")
    @GetMapping("/announce/latest")
    public R<List<SysAnnounceDTO>> listLatestAnnounces(
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        int size = limit == null || limit <= 0 || limit > 5 ? 5 : limit;
        List<SysAnnounceDO> announces = sysAnnounceMapper.selectList(
                new LambdaQueryWrapper<SysAnnounceDO>()
                        .eq(SysAnnounceDO::getStatus, 1)
                        .orderByDesc(SysAnnounceDO::getCreateTime)
                        .last("LIMIT " + size));
        return R.ok(announces.stream().map(this::toDTO).toList());
    }

    /** 清理超保留期的私聊消息 */
    @Operation(summary = "清理")
    @PostMapping("/purge/chat")
    public R<Integer> purgeOldChatMessages(
            @RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays) {
        int days = retentionDays == null || retentionDays <= 0 ? 180 : retentionDays;
        return R.ok(chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessageDO>()
                .lt(ChatMessageDO::getSendTime, LocalDateTime.now().minusDays(days))));
    }

    /** 清理超保留期的群聊消息 */
    @Operation(summary = "清理")
    @PostMapping("/purge/group-chat")
    public R<Integer> purgeOldGroupChatMessages(
            @RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays) {
        int days = retentionDays == null || retentionDays <= 0 ? 180 : retentionDays;
        return R.ok(chatGroupMessageMapper.delete(new LambdaQueryWrapper<ChatGroupMessageDO>()
                .lt(ChatGroupMessageDO::getSendTime, LocalDateTime.now().minusDays(days))));
    }

    /** 清理超保留期的已读公告阅读记录 */
    @Operation(summary = "清理")
    @PostMapping("/purge/read-notice")
    public R<Integer> purgeReadNotices(
            @RequestParam(value = "retentionDays", defaultValue = "180") Integer retentionDays) {
        int days = retentionDays == null || retentionDays <= 0 ? 180 : retentionDays;
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        return R.ok(userAnnounceMapper.delete(new LambdaQueryWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getIsRead, 1)
                .and(w -> w.lt(UserAnnounceDO::getReadTime, deadline)
                        .or(n -> n.isNull(UserAnnounceDO::getReadTime)
                                .lt(UserAnnounceDO::getCreateTime, deadline)))));
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
