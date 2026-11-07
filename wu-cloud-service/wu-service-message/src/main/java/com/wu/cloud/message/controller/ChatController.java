package com.wu.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.message.entity.ChatGroupDO;
import com.wu.cloud.message.entity.ChatGroupMessageDO;
import com.wu.cloud.message.entity.ChatMessageDO;
import com.wu.cloud.message.service.AnnounceService;
import com.wu.cloud.message.service.ChatService;
import com.wu.cloud.message.websocket.MessageWebSocketHandler;
import com.wu.cloud.message.vo.ChatGroupLogVO;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "企业IM")
@RestController
@RequestMapping("/api/message/chat")
public class ChatController {

    @Resource
    private ChatService chatService;
    @Lazy
    @Resource
    private RemoteFileService remoteFileService;

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFileDTO> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            R<SysFileDTO> res = remoteFileService.uploadFile(file, "chat-image");
            if (res != null && res.getCode() == 200) {
                return res;
            }
        } catch (Exception e) {
            log.warn("图片上传调用文件服务失败: {}", e.getMessage());
        }
        return R.fail(500, "文件服务不可用，图片上传失败，请稍后再试");
    }

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFileDTO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            R<SysFileDTO> res = remoteFileService.uploadFile(file, "chat-file");
            if (res != null && res.getCode() == 200) {
                return res;
            }
        } catch (Exception e) {
            log.warn("文件上传调用文件服务失败: {}", e.getMessage());
        }
        return R.fail(500, "文件服务不可用，文件上传失败，请稍后再试");
    }

    @Operation(summary = "新增")
    @GetMapping("/can-create-group")
    public R<Boolean> canCreateGroup() {
        return R.ok(chatService.canCreateGroup(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "消息操作")
    @PostMapping("/recall/{messageId}")
    public R<ChatMessageDO> recallPrivate(@PathVariable("messageId") Long messageId) {
        return R.ok(chatService.recallPrivate(StpUtil.getLoginIdAsLong(), messageId));
    }

    @Operation(summary = "获取详情")
    @PostMapping("/typing/{targetUserId}")
    public R<Boolean> typing(@PathVariable("targetUserId") Long targetUserId) {
        chatService.relayTyping(StpUtil.getLoginIdAsLong(), targetUserId);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/send")
    public R<ChatMessageDO> send(@Validated @RequestBody SendReq req) {
        return R.ok(chatService.sendPrivate(
                StpUtil.getLoginIdAsLong(), req.getReceiverId(), req.getContent(), req.getMsgType()));
    }

    @Operation(summary = "获取详情")
    @GetMapping("/history/{targetId}")
    public R<PageResult<ChatMessageDO>> history(@PathVariable("targetId") Long targetId,
                                                @RequestParam(defaultValue = "1") int pageNo,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        Page<ChatMessageDO> page = chatService.history(StpUtil.getLoginIdAsLong(), targetId, pageNo, pageSize);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "获取详情")
    @GetMapping("/users")
    public R<List<Map<String, Object>>> users() {
        return R.ok(chatService.listUsers(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read/{senderId}")
    public R<Boolean> read(@PathVariable("senderId") Long senderId) {
        chatService.markRead(StpUtil.getLoginIdAsLong(), senderId);
        return R.ok(true);
    }

    @Operation(summary = "我的未读数")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(chatService.unreadCount(StpUtil.getLoginIdAsLong()));
    }

    @Resource
    private MessageWebSocketHandler webSocketHandler;

    @Operation(summary = "获取详情")
    @GetMapping("/online/{userId}")
    public R<Boolean> online(@PathVariable("userId") Long userId) {
        return R.ok(webSocketHandler.isOnline(userId));
    }

    @Operation(summary = "获取详情")
    @DeleteMapping("/clear/{targetId}")
    public R<Boolean> clear(@PathVariable("targetId") Long targetId) {
        chatService.clearHistory(StpUtil.getLoginIdAsLong(), targetId);
        return R.ok(true);
    }

    @Operation(summary = "解锁")
    @PostMapping("/block/{targetId}")
    public R<Boolean> block(@PathVariable("targetId") Long targetId) {
        chatService.block(StpUtil.getLoginIdAsLong(), targetId);
        return R.ok(true);
    }

    @Operation(summary = "解锁")
    @DeleteMapping("/block/{targetId}")
    public R<Boolean> unblock(@PathVariable("targetId") Long targetId) {
        chatService.unblock(StpUtil.getLoginIdAsLong(), targetId);
        return R.ok(true);
    }

    @Data
    public static class SendReq {
        @NotNull(message = "接收者ID不能为空")
        private Long receiverId;
        @NotBlank(message = "消息内容不能为空")
        private String content;
        private Integer msgType;
        private List<Long> mentionIds;
    }
}

@Tag(name = "群聊")
@RestController
@RequestMapping("/api/message/chat/group")
class ChatGroupController {

    @Resource
    private ChatService chatService;

    @Operation(summary = "获取详情")
    @GetMapping("/{groupId}")
    public R<Map<String, Object>> detail(@PathVariable("groupId") Long groupId) {
        return R.ok(chatService.getGroupContext(groupId, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{groupId}/announcement/read")
    public R<Boolean> readAnnouncement(@PathVariable("groupId") Long groupId) {
        chatService.markAnnouncementRead(groupId, StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @PostMapping("/{groupId}/notify-muted")
    public R<Boolean> setNotifyMuted(@PathVariable("groupId") Long groupId,
                                      @RequestParam(defaultValue = "true") boolean muted) {
        chatService.setNotifyMuted(groupId, StpUtil.getLoginIdAsLong(), muted);
        return R.ok(true);
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/update")
    public R<Boolean> update(@Validated @RequestBody UpdateGroupReq req) {
        chatService.updateGroup(req.getId(), StpUtil.getLoginIdAsLong(), req.getName(), req.getAnnouncement());
        return R.ok(true);
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<ChatGroupDO> create(@Validated @RequestBody CreateGroupReq req) {
        return R.ok(chatService.createGroup(
                StpUtil.getLoginIdAsLong(), req.getName(), req.getMemberIds()));
    }

    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(chatService.myGroups(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "消息操作")
    @PostMapping("/{groupId}/message")
    public R<ChatGroupMessageDO> send(@PathVariable("groupId") Long groupId, @Validated @RequestBody GroupSendReq req) {
        return R.ok(chatService.sendGroupMessage(
                groupId, StpUtil.getLoginIdAsLong(), req.getContent(), req.getMsgType(), req.getMentionIds()));
    }

    @Operation(summary = "消息操作")
    @PostMapping("/{groupId}/message/{messageId}/recall")
    public R<ChatGroupMessageDO> recallGroupMessage(@PathVariable("groupId") Long groupId,
                                                     @PathVariable("messageId") Long messageId) {
        return R.ok(chatService.recallGroupMessage(
                groupId, StpUtil.getLoginIdAsLong(), messageId));
    }

    @Operation(summary = "消息操作")
    @GetMapping("/{groupId}/messages")
    public R<PageResult<ChatGroupMessageDO>> messages(@PathVariable("groupId") Long groupId,
                                                       @RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        Page<ChatGroupMessageDO> page = chatService.groupMessages(groupId, StpUtil.getLoginIdAsLong(), pageNo, pageSize);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "群组操作")
    @GetMapping("/{groupId}/members")
    public R<List<Map<String, Object>>> members(@PathVariable("groupId") Long groupId) {
        return R.ok(chatService.groupMembers(groupId));
    }

    @Operation(summary = "群组操作")
    @GetMapping("/{groupId}/logs")
    public R<List<ChatGroupLogVO>> logs(@PathVariable("groupId") Long groupId) {
        return R.ok(chatService.groupLogs(groupId, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "新增")
    @PostMapping("/{groupId}/members")
    public R<Boolean> addMembers(@PathVariable("groupId") Long groupId, @Validated @RequestBody MemberIdsReq req) {
        chatService.addMembers(groupId, req.getUserIds(), StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Operation(summary = "移动")
    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public R<Boolean> removeMember(@PathVariable("groupId") Long groupId, @PathVariable("memberUserId") Long memberUserId) {
        chatService.removeMember(groupId, memberUserId, StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @PostMapping("/{groupId}/admin/{memberUserId}")
    public R<Boolean> setAdmin(@PathVariable("groupId") Long groupId, @PathVariable("memberUserId") Long memberUserId,
                                @RequestParam(defaultValue = "true") boolean admin) {
        chatService.setAdmin(groupId, memberUserId, StpUtil.getLoginIdAsLong(), admin);
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @PostMapping("/{groupId}/mute/{memberUserId}")
    public R<Boolean> setMuted(@PathVariable("groupId") Long groupId, @PathVariable("memberUserId") Long memberUserId,
                                @RequestParam(defaultValue = "true") boolean muted) {
        chatService.setMuted(groupId, memberUserId, StpUtil.getLoginIdAsLong(), muted);
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @PostMapping("/{groupId}/transfer/{newOwnerId}")
    public R<Boolean> transferOwner(@PathVariable("groupId") Long groupId, @PathVariable("newOwnerId") Long newOwnerId) {
        chatService.transferOwner(groupId, newOwnerId, StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @PostMapping("/{groupId}/quit")
    public R<Boolean> quit(@PathVariable("groupId") Long groupId) {
        chatService.quitGroup(groupId, StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Operation(summary = "群组操作")
    @DeleteMapping("/{groupId}")
    public R<Boolean> dissolve(@PathVariable("groupId") Long groupId) {
        chatService.dissolveGroup(groupId, StpUtil.getLoginIdAsLong());
        return R.ok(true);
    }

    @Data
    public static class CreateGroupReq {
        @NotBlank(message = "群名称不能为空")
        @Size(max = 50, message = "群名称最多 50 个字符")
        private String name;
        private List<Long> memberIds;
    }

    @Data
    public static class UpdateGroupReq {
        @NotNull(message = "群ID不能为空")
        private Long id;
        @NotBlank(message = "群名称不能为空")
        @Size(max = 50, message = "群名称最多 50 个字符")
        private String name;
        private String announcement;
    }

    @Data
    public static class MemberIdsReq {
        @NotNull(message = "成员ID不能为空")
        private List<Long> userIds;
    }

    @Data
    public static class GroupSendReq {
        @NotBlank(message = "消息内容不能为空")
        private String content;
        private Integer msgType;
        private List<Long> mentionIds;
    }
}

@Tag(name = "消息汇总")
@RestController
@RequestMapping("/api/message")
class MessageCenterController {

    @Resource
    private AnnounceService announceService;
    @Resource
    private ChatService chatService;
    @Lazy
    @Resource
    private com.wu.cloud.api.ticket.feign.RemoteTicketService remoteTicketService;

    @Operation(summary = "回收站汇总统计")
    @GetMapping("/summary")
    public R<Map<String, Long>> summary() {
        Long userId = StpUtil.getLoginIdAsLong();
        long announce = announceService.unreadCount(userId);
        long chat = chatService.unreadCount(userId);
        long inbox = 0L;
        try {
            R<Long> inboxRes = remoteTicketService.getNoticeUnreadCount(userId);
            if (inboxRes != null && inboxRes.getCode() == 200 && inboxRes.getData() != null) {
                inbox = inboxRes.getData();
            }
        } catch (Exception ignored) {}

        Map<String, Long> map = new HashMap<>();
        map.put("inboxCount", inbox);
        map.put("announceCount", announce);
        map.put("chatCount", chat);
        map.put("noticeCount", inbox + announce);
        map.put("total", inbox + announce + chat);
        return R.ok(map);
    }
}
