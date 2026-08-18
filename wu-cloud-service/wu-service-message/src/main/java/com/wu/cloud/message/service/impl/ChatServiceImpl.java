package com.wu.cloud.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.message.entity.*;
import com.wu.cloud.message.mapper.*;
import com.wu.cloud.message.service.ChatService;
import com.wu.cloud.message.vo.*;
import com.wu.cloud.message.websocket.MessageWebSocketHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int RECALL_MINUTES = 2;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private UserBlacklistMapper blacklistMapper;
    @Resource
    private ChatGroupMapper chatGroupMapper;
    @Resource
    private ChatGroupMemberMapper groupMemberMapper;
    @Resource
    private ChatGroupMessageMapper groupMessageMapper;
    @Resource
    private ChatGroupLogMapper groupLogMapper;
    @Resource
    private MessageWebSocketHandler webSocketHandler;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean canCreateGroup(Long userId) {
        // 仅超级管理员可创建群聊（角色由网关注入线程上下文；对齐单体 PermissionService.hasRole(userId, super_admin)）
        if (userId == null || userId <= 0) {
            return false;
        }
        try {
            return UserContextHolder.getRoles() != null
                    && UserContextHolder.getRoles().contains(PermissionChecker.SUPER_ADMIN_ROLE);
        } catch (Exception e) {
            log.warn("AI/canCreateGroup 角色读取异常，按无权限处理: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isBlocked(Long senderId, Long receiverId) {
        Long c1 = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, receiverId)
                .eq(UserBlacklistDO::getBlockedUserId, senderId));
        Long c2 = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, senderId)
                .eq(UserBlacklistDO::getBlockedUserId, receiverId));
        return c1 > 0 || c2 > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageDO sendPrivate(Long senderId, Long receiverId, String content, Integer msgType) {
        if (isBlocked(senderId, receiverId)) {
            throw new BusinessException(400, "无法发送，存在拉黑关系");
        }
        SysUserDTO sender = getUserById(senderId);
        ChatMessageDO msg = new ChatMessageDO();
        msg.setSenderId(senderId);
        msg.setSenderName(sender != null ? sender.getNickname() : "");
        msg.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMsgType(msgType == null ? 1 : msgType);
        msg.setIsRead(0);
        msg.setSendTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("senderId", msg.getSenderId());
        payload.put("senderName", msg.getSenderName());
        payload.put("senderAvatar", msg.getSenderAvatar());
        payload.put("content", msg.getContent());
        payload.put("msgType", msg.getMsgType());
        runAfterCommit(() -> {
            webSocketHandler.sendChatPayload(receiverId, payload);
            webSocketHandler.sendTypingStopPayload(receiverId, senderId);
        });
        return msg;
    }

    @Override
    public Page<ChatMessageDO> history(Long userId, Long targetId, int pageNo, int pageSize) {
        LambdaQueryWrapper<ChatMessageDO> w = new LambdaQueryWrapper<ChatMessageDO>()
                .and(q -> q.and(a -> a.eq(ChatMessageDO::getSenderId, userId).eq(ChatMessageDO::getReceiverId, targetId))
                        .or(b -> b.eq(ChatMessageDO::getSenderId, targetId).eq(ChatMessageDO::getReceiverId, userId)))
                .orderByDesc(ChatMessageDO::getSendTime);
        Page<ChatMessageDO> page = chatMessageMapper.selectPage(new Page<>(pageNo, pageSize), w);
        Collections.reverse(page.getRecords());
        return page;
    }

    @Override
    public List<Map<String, Object>> listUsers(Long userId) {
        List<SysUserDTO> users = listActiveUsersExcept(userId);
        if (users.isEmpty()) {
            return List.of();
        }

        Set<Long> blockedIds = new HashSet<>(blacklistMapper.selectBlockedUserIds(userId));
        Map<Long, ChatPeerLatestVO> latestByPeer = chatMessageMapper.selectLatestPerPeer(userId).stream()
                .collect(Collectors.toMap(ChatPeerLatestVO::getPeerId, v -> v, (a, b) -> a));
        Map<Long, Long> unreadBySender = chatMessageMapper.selectUnreadCountPerSender(userId).stream()
                .collect(Collectors.toMap(ChatPeerUnreadVO::getSenderId, ChatPeerUnreadVO::getUnreadCount));

        List<Map<String, Object>> result = new ArrayList<>(users.size());
        for (SysUserDTO u : users) {
            Long peerId = u.getUserId();
            Map<String, Object> item = new HashMap<>();
            item.put("id", peerId);
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("avatar", u.getAvatar());
            item.put("isBlocked", blockedIds.contains(peerId));
            item.put("online", webSocketHandler.isOnline(peerId));
            ChatPeerLatestVO latest = latestByPeer.get(peerId);
            if (latest != null) {
                item.put("lastMessage", ChatMsgType.previewLabel(latest.getMsgType(), latest.getContent()));
                item.put("lastMessageTime", latest.getSendTime());
            }
            item.put("unreadCount", unreadBySender.getOrDefault(peerId, 0L));
            result.add(item);
        }
        result.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastMessageTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastMessageTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long senderId) {
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessageDO>()
                .eq(ChatMessageDO::getReceiverId, userId)
                .eq(ChatMessageDO::getSenderId, senderId)
                .eq(ChatMessageDO::getIsRead, 0)
                .set(ChatMessageDO::getIsRead, 1));
    }

    @Override
    public long unreadCount(Long userId) {
        return chatMessageMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageDO recallPrivate(Long userId, Long messageId) {
        ChatMessageDO msg = chatMessageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!userId.equals(msg.getSenderId())) {
            throw new BusinessException(403, "只能撤回自己发送的消息");
        }
        long minutes = Duration.between(msg.getSendTime(), LocalDateTime.now()).toMinutes();
        if (minutes >= RECALL_MINUTES) {
            throw new BusinessException(400, "消息已发送超过 2 分钟，无法撤回");
        }
        chatMessageMapper.deleteById(messageId);
        Long rx = msg.getReceiverId();
        runAfterCommit(() -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "chat");
            payload.put("recall", true);
            payload.put("messageId", messageId);
            payload.put("fromUserId", userId);
            payload.put("time", System.currentTimeMillis());
            webSocketHandler.sendChatPayload(rx, payload);
        });
        return msg;
    }

    @Override
    public void relayTyping(Long fromUserId, Long toUserId) {
        webSocketHandler.sendTypingPayload(toUserId, fromUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId, Long targetId) {
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessageDO>()
                .and(q -> q.and(a -> a.eq(ChatMessageDO::getSenderId, userId).eq(ChatMessageDO::getReceiverId, targetId))
                        .or(b -> b.eq(ChatMessageDO::getSenderId, targetId).eq(ChatMessageDO::getReceiverId, userId))));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void block(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw new BusinessException(400, "不能拉黑自己");
        }
        Long count = blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, userId)
                .eq(UserBlacklistDO::getBlockedUserId, targetId));
        if (count == 0) {
            UserBlacklistDO b = new UserBlacklistDO();
            b.setUserId(userId);
            b.setBlockedUserId(targetId);
            b.setCreateTime(LocalDateTime.now());
            blacklistMapper.insert(b);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblock(Long userId, Long targetId) {
        blacklistMapper.delete(new LambdaQueryWrapper<UserBlacklistDO>()
                .eq(UserBlacklistDO::getUserId, userId)
                .eq(UserBlacklistDO::getBlockedUserId, targetId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupDO createGroup(Long ownerId, String name, List<Long> memberIds) {
        if (!canCreateGroup(ownerId)) {
            throw new BusinessException(403, "仅超级管理员可创建群聊");
        }
        ChatGroupDO group = new ChatGroupDO();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setMaxMembers(200);
        group.setStatus(1);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.insert(group);

        Set<Long> uids = new HashSet<>();
        uids.add(ownerId);
        if (memberIds != null) {
            uids.addAll(memberIds);
        }
        LocalDateTime now = LocalDateTime.now();
        List<ChatGroupMemberDO> members = new ArrayList<>();
        for (Long uid : uids) {
            if (uid == null || uid <= 0) continue;
            ChatGroupMemberDO m = new ChatGroupMemberDO();
            m.setGroupId(group.getId());
            m.setUserId(uid);
            m.setRole(uid.equals(ownerId) ? 2 : 0);
            m.setJoinTime(now);
            members.add(m);
        }
        if (!members.isEmpty()) {
            groupMemberMapper.insertBatch(members);
        }

        SysUserDTO owner = getUserById(ownerId);
        String ownerName = owner != null ? owner.getNickname() : "用户" + ownerId;
        logGroupAction(group.getId(), "CREATE", ownerId, ownerName, null, null, "创建群聊「" + name + "」");
        return group;
    }

    @Override
    public Map<String, Object> getGroupContext(Long groupId, Long userId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() == 0) {
            throw new BusinessException(404, "群聊不存在或已解散");
        }
        ChatGroupMemberDO me = getMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(403, "你不是该群成员");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("avatar", group.getAvatar());
        map.put("ownerId", group.getOwnerId());
        map.put("announcement", group.getAnnouncement());
        map.put("maxMembers", group.getMaxMembers());
        map.put("myRole", me.getRole());
        map.put("myMuted", me.getMuted());
        map.put("myNotifyMuted", me.getNotifyMuted() != null && me.getNotifyMuted() == 1);
        map.put("announcementReadTime", me.getAnnouncementReadTime());
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAnnouncementRead(Long groupId, Long userId) {
        groupMemberMapper.update(null, new LambdaUpdateWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId)
                .set(ChatGroupMemberDO::getAnnouncementReadTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNotifyMuted(Long groupId, Long userId, boolean muted) {
        groupMemberMapper.update(null, new LambdaUpdateWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId)
                .set(ChatGroupMemberDO::getNotifyMuted, muted ? 1 : 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, Long operatorId, String name, String announcement) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException(404, "群聊不存在或已解散");
        }
        checkGroupAdminOrOwner(groupId, operatorId);
        boolean changed = false;
        if (StringUtils.hasText(name) && !name.equals(group.getName())) {
            group.setName(name);
            changed = true;
        }
        if (announcement != null && !announcement.equals(group.getAnnouncement())) {
            group.setAnnouncement(announcement);
            changed = true;
        }
        if (changed) {
            group.setUpdateTime(LocalDateTime.now());
            chatGroupMapper.updateById(group);
            SysUserDTO op = getUserById(operatorId);
            logGroupAction(groupId, "UPDATE", operatorId, op != null ? op.getNickname() : null, null, null, "修改群资料");
        }
    }

    @Override
    public List<Map<String, Object>> myGroups(Long userId) {
        List<ChatGroupMemberDO> myMemberships = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getUserId, userId));
        if (myMemberships.isEmpty()) {
            return List.of();
        }
        Set<Long> gids = myMemberships.stream().map(ChatGroupMemberDO::getGroupId).collect(Collectors.toSet());
        List<ChatGroupDO> groups = chatGroupMapper.selectByIds(gids).stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .collect(Collectors.toList());
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> countMap = groupMemberMapper.selectMemberCountByGroupIds(gids).stream()
                .collect(Collectors.toMap(ChatGroupMemberCountVO::getGroupId, ChatGroupMemberCountVO::getMemberCount));
        Map<Long, ChatGroupLatestVO> latestMap = groupMessageMapper.selectLatestByGroupIds(gids).stream()
                .collect(Collectors.toMap(ChatGroupLatestVO::getGroupId, v -> v, (a, b) -> a));

        List<Map<String, Object>> res = new ArrayList<>();
        for (ChatGroupDO g : groups) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("avatar", g.getAvatar());
            item.put("ownerId", g.getOwnerId());
            item.put("announcement", g.getAnnouncement());
            item.put("memberCount", countMap.getOrDefault(g.getId(), 0L));
            ChatGroupLatestVO latest = latestMap.get(g.getId());
            if (latest != null) {
                item.put("lastMessage", ChatMsgType.previewLabel(latest.getMsgType(), latest.getContent()));
                item.put("lastMessageTime", latest.getSendTime());
            }
            res.add(item);
        }
        res.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastMessageTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastMessageTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO sendGroupMessage(Long groupId, Long senderId, String content, Integer msgType, List<Long> mentionIds) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException(404, "群聊不存在或已解散");
        }
        ChatGroupMemberDO me = getMember(groupId, senderId);
        if (me == null) {
            throw new BusinessException(403, "你不在此群内");
        }
        if (me.getMuted() != null && me.getMuted() == 1) {
            throw new BusinessException(400, "你已被在该群禁言");
        }
        SysUserDTO sender = getUserById(senderId);
        ChatGroupMessageDO msg = new ChatGroupMessageDO();
        msg.setGroupId(groupId);
        msg.setSenderId(senderId);
        msg.setSenderName(sender != null ? sender.getNickname() : me.getNickname());
        msg.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        msg.setContent(content);
        msg.setMsgType(msgType == null ? 1 : msgType);
        if (mentionIds != null && !mentionIds.isEmpty()) {
            try {
                msg.setMentionIds(objectMapper.writeValueAsString(mentionIds));
            } catch (Exception ignored) {}
        }
        msg.setSendTime(LocalDateTime.now());
        groupMessageMapper.insert(msg);

        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        // 被 @ 的用户集合（含发送者，用于判断 atMe）
        Set<Long> mentionIdSet = (mentionIds != null && !mentionIds.isEmpty()) ? new HashSet<>(mentionIds) : Set.of();
        runAfterCommit(() -> {
            for (ChatGroupMemberDO member : members) {
                Long memberUserId = member.getUserId();
                if (memberUserId.equals(senderId)) {
                    continue;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "groupChat");
                payload.put("groupId", groupId);
                payload.put("id", msg.getId());
                payload.put("senderId", senderId);
                payload.put("senderName", msg.getSenderName());
                payload.put("senderAvatar", msg.getSenderAvatar());
                payload.put("content", msg.getContent());
                payload.put("msgType", msg.getMsgType());
                payload.put("mentionIds", mentionIds);
                // 差异化 @ 提醒：被 @ 的成员置 atMe=true，前端据此振动/角标提醒
                payload.put("atMe", mentionIdSet.contains(memberUserId));
                webSocketHandler.sendGroupChatPayload(memberUserId, payload);
            }
        });
        return msg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessageDO recallGroupMessage(Long groupId, Long operatorId, Long messageId) {
        ChatGroupMessageDO msg = groupMessageMapper.selectById(messageId);
        if (msg == null || !groupId.equals(msg.getGroupId())) {
            throw new BusinessException(404, "消息不存在");
        }
        boolean isSelf = operatorId.equals(msg.getSenderId());
        if (isSelf) {
            long minutes = Duration.between(msg.getSendTime(), LocalDateTime.now()).toMinutes();
            if (minutes >= RECALL_MINUTES) {
                throw new BusinessException(400, "消息已发送超过 2 分钟，无法撤回");
            }
        } else {
            checkGroupAdminOrOwner(groupId, operatorId);
        }
        groupMessageMapper.deleteById(messageId);
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        runAfterCommit(() -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "groupChat");
            payload.put("recall", true);
            payload.put("groupId", groupId);
            payload.put("messageId", messageId);
            payload.put("operatorId", operatorId);

            for (ChatGroupMemberDO member : members) {
                webSocketHandler.sendGroupChatPayload(member.getUserId(), new HashMap<>(payload));
            }
        });
        return msg;
    }

    @Override
    public Page<ChatGroupMessageDO> groupMessages(Long groupId, Long userId, int pageNo, int pageSize) {
        ChatGroupMemberDO me = getMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(403, "你不是该群成员");
        }
        Page<ChatGroupMessageDO> page = groupMessageMapper.selectPage(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<ChatGroupMessageDO>()
                        .eq(ChatGroupMessageDO::getGroupId, groupId)
                        .orderByDesc(ChatGroupMessageDO::getSendTime));
        Collections.reverse(page.getRecords());
        return page;
    }

    @Override
    public List<Map<String, Object>> groupMembers(Long groupId) {
        List<ChatGroupMemberDO> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (ChatGroupMemberDO m : members) {
            SysUserDTO u = getUserById(m.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("userId", m.getUserId());
            item.put("username", u != null ? u.getUsername() : "用户" + m.getUserId());
            item.put("nickname", u != null ? u.getNickname() : m.getNickname());
            item.put("avatar", u != null ? u.getAvatar() : null);
            item.put("role", m.getRole());
            item.put("muted", m.getMuted());
            item.put("notifyMuted", m.getNotifyMuted());
            item.put("joinTime", m.getJoinTime());
            res.add(item);
        }
        return res;
    }

    @Override
    public List<ChatGroupLogVO> groupLogs(Long groupId, Long userId) {
        ChatGroupMemberDO me = getMember(groupId, userId);
        if (me == null) {
            throw new BusinessException(403, "你不是该群成员");
        }
        List<ChatGroupLogDO> logs = groupLogMapper.selectList(
                new LambdaQueryWrapper<ChatGroupLogDO>()
                        .eq(ChatGroupLogDO::getGroupId, groupId)
                        .orderByDesc(ChatGroupLogDO::getCreateTime));
        List<ChatGroupLogVO> res = new ArrayList<>();
        for (ChatGroupLogDO l : logs) {
            ChatGroupLogVO vo = new ChatGroupLogVO();
            vo.setId(l.getId());
            vo.setGroupId(l.getGroupId());
            vo.setActionType(l.getActionType());
            vo.setOperatorId(l.getOperatorId());
            vo.setOperatorName(l.getOperatorName());
            vo.setTargetUserId(l.getTargetUserId());
            vo.setTargetUserName(l.getTargetUserName());
            vo.setDetail(l.getDetail());
            vo.setCreateTime(l.getCreateTime());
            vo.buildContent();
            res.add(vo);
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long groupId, List<Long> userIds, Long operatorId) {
        checkGroupAdminOrOwner(groupId, operatorId);
        Set<Long> existing = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMemberDO>().eq(ChatGroupMemberDO::getGroupId, groupId))
                .stream().map(ChatGroupMemberDO::getUserId).collect(Collectors.toSet());
        List<ChatGroupMemberDO> toAdd = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : userIds) {
            if (uid != null && !existing.contains(uid)) {
                ChatGroupMemberDO m = new ChatGroupMemberDO();
                m.setGroupId(groupId);
                m.setUserId(uid);
                m.setRole(0);
                m.setJoinTime(now);
                toAdd.add(m);
            }
        }
        if (!toAdd.isEmpty()) {
            groupMemberMapper.insertBatch(toAdd);
            SysUserDTO op = getUserById(operatorId);
            logGroupAction(groupId, "INVITE", operatorId, op != null ? op.getNickname() : null, null, null, "邀请 " + toAdd.size() + " 名新成员");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long memberUserId, Long operatorId) {
        checkGroupAdminOrOwner(groupId, operatorId);
        ChatGroupMemberDO target = getMember(groupId, memberUserId);
        if (target == null) {
            throw new BusinessException(404, "目标用户不是该群成员");
        }
        if (target.getRole() != null && target.getRole() == 2) {
            throw new BusinessException(400, "不能移除群主");
        }
        groupMemberMapper.deleteById(target.getId());
        SysUserDTO op = getUserById(operatorId);
        SysUserDTO tu = getUserById(memberUserId);
        logGroupAction(groupId, "REMOVE", operatorId, op != null ? op.getNickname() : null,
                memberUserId, tu != null ? tu.getNickname() : null, "移出了群成员");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long groupId, Long memberUserId, Long operatorId, boolean admin) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getOwnerId().equals(operatorId)) {
            throw new BusinessException(403, "只有群主可以设置或取消管理员");
        }
        ChatGroupMemberDO target = getMember(groupId, memberUserId);
        if (target == null) {
            throw new BusinessException(404, "目标用户不是该群成员");
        }
        target.setRole(admin ? 1 : 0);
        groupMemberMapper.updateById(target);
        SysUserDTO op = getUserById(operatorId);
        SysUserDTO tu = getUserById(memberUserId);
        logGroupAction(groupId, admin ? "SET_ADMIN" : "REMOVE_ADMIN", operatorId, op != null ? op.getNickname() : null,
                memberUserId, tu != null ? tu.getNickname() : null, admin ? "设为管理员" : "取消管理员");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMuted(Long groupId, Long memberUserId, Long operatorId, boolean muted) {
        checkGroupAdminOrOwner(groupId, operatorId);
        ChatGroupMemberDO target = getMember(groupId, memberUserId);
        if (target == null) {
            throw new BusinessException(404, "目标用户不是该群成员");
        }
        target.setMuted(muted ? 1 : 0);
        groupMemberMapper.updateById(target);
        SysUserDTO op = getUserById(operatorId);
        SysUserDTO tu = getUserById(memberUserId);
        logGroupAction(groupId, muted ? "MUTE" : "UNMUTE", operatorId, op != null ? op.getNickname() : null,
                memberUserId, tu != null ? tu.getNickname() : null, muted ? "禁言成员" : "解除禁言");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long groupId, Long newOwnerId, Long operatorId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || !group.getOwnerId().equals(operatorId)) {
            throw new BusinessException(403, "只有群主可以转让群");
        }
        ChatGroupMemberDO newOwner = getMember(groupId, newOwnerId);
        if (newOwner == null) {
            throw new BusinessException(404, "目标用户不是该群成员");
        }
        ChatGroupMemberDO oldOwner = getMember(groupId, operatorId);
        if (oldOwner != null) {
            oldOwner.setRole(0);
            groupMemberMapper.updateById(oldOwner);
        }
        newOwner.setRole(2);
        groupMemberMapper.updateById(newOwner);

        group.setOwnerId(newOwnerId);
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.updateById(group);

        SysUserDTO op = getUserById(operatorId);
        SysUserDTO tu = getUserById(newOwnerId);
        logGroupAction(groupId, "TRANSFER_OWNER", operatorId, op != null ? op.getNickname() : null,
                newOwnerId, tu != null ? tu.getNickname() : null, "转让群主");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId, Long userId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException(404, "群聊不存在");
        }
        if (group.getOwnerId().equals(userId)) {
            throw new BusinessException(400, "群主不能直接退群，请先转让群主或解散群聊");
        }
        ChatGroupMemberDO me = getMember(groupId, userId);
        if (me != null) {
            groupMemberMapper.deleteById(me.getId());
            SysUserDTO op = getUserById(userId);
            logGroupAction(groupId, "QUIT", userId, op != null ? op.getNickname() : null, null, null, "退出群聊");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId, Long operatorId) {
        ChatGroupDO group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new BusinessException(404, "群聊不存在");
        }
        if (!group.getOwnerId().equals(operatorId)) {
            throw new BusinessException(403, "只有群主可以解散群聊");
        }
        group.setStatus(0);
        group.setUpdateTime(LocalDateTime.now());
        chatGroupMapper.updateById(group);

        SysUserDTO op = getUserById(operatorId);
        logGroupAction(groupId, "DISSOLVE", operatorId, op != null ? op.getNickname() : null, null, null, "解散群聊");
    }

    private ChatGroupMemberDO getMember(Long groupId, Long userId) {
        return groupMemberMapper.selectOne(new LambdaQueryWrapper<ChatGroupMemberDO>()
                .eq(ChatGroupMemberDO::getGroupId, groupId)
                .eq(ChatGroupMemberDO::getUserId, userId));
    }

    private void checkGroupAdminOrOwner(Long groupId, Long operatorId) {
        ChatGroupMemberDO me = getMember(groupId, operatorId);
        if (me == null || (me.getRole() != 1 && me.getRole() != 2)) {
            throw new BusinessException(403, "只有群主或管理员有权操作");
        }
    }

    private void logGroupAction(Long groupId, String actionType, Long operatorId, String operatorName,
                                Long targetUserId, String targetUserName, String detail) {
        ChatGroupLogDO logEntry = new ChatGroupLogDO();
        logEntry.setGroupId(groupId);
        logEntry.setActionType(actionType);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setTargetUserId(targetUserId);
        logEntry.setTargetUserName(targetUserName);
        logEntry.setDetail(detail);
        logEntry.setCreateTime(LocalDateTime.now());
        groupLogMapper.insert(logEntry);
    }

    /** 对齐单体：message 与 system 共用业务库，直接 JDBC 查 sys_user，避免 Feign 失败影响发消息 */
    private SysUserDTO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            List<SysUserDTO> rows = jdbcTemplate.query(
                    "SELECT id, username, nickname, avatar FROM sys_user WHERE id = ? AND deleted = 0 LIMIT 1",
                    (rs, rowNum) -> mapUserRow(rs),
                    userId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("JDBC getUserById failed userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<SysUserDTO> listActiveUsersExcept(Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    "SELECT id, username, nickname, avatar FROM sys_user WHERE status = 1 AND deleted = 0 AND id <> ? ORDER BY id",
                    (rs, rowNum) -> mapUserRow(rs),
                    userId);
        } catch (Exception e) {
            log.warn("JDBC listActiveUsersExcept failed userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private SysUserDTO mapUserRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        SysUserDTO dto = new SysUserDTO();
        dto.setUserId(rs.getLong("id"));
        dto.setUsername(rs.getString("username"));
        dto.setNickname(rs.getString("nickname"));
        dto.setAvatar(rs.getString("avatar"));
        return dto;
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        log.warn("事务提交后回调失败（消息已入库）: {}", e.getMessage());
                    }
                }
            });
        } else {
            try {
                runnable.run();
            } catch (Exception e) {
                log.warn("事务外回调失败: {}", e.getMessage());
            }
        }
    }
}
