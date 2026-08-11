package com.wu.cloud.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.message.entity.ChatGroupDO;
import com.wu.cloud.message.entity.ChatGroupMessageDO;
import com.wu.cloud.message.entity.ChatMessageDO;
import com.wu.cloud.message.vo.ChatGroupLogVO;

import java.util.List;
import java.util.Map;

public interface ChatService {

    boolean canCreateGroup(Long userId);

    boolean isBlocked(Long senderId, Long receiverId);

    ChatMessageDO sendPrivate(Long senderId, Long receiverId, String content, Integer msgType);

    Page<ChatMessageDO> history(Long userId, Long targetId, int pageNo, int pageSize);

    List<Map<String, Object>> listUsers(Long userId);

    void markRead(Long userId, Long senderId);

    long unreadCount(Long userId);

    ChatMessageDO recallPrivate(Long userId, Long messageId);

    void relayTyping(Long fromUserId, Long toUserId);

    void clearHistory(Long userId, Long targetId);

    void block(Long userId, Long targetId);

    void unblock(Long userId, Long targetId);

    ChatGroupDO createGroup(Long ownerId, String name, List<Long> memberIds);

    Map<String, Object> getGroupContext(Long groupId, Long userId);

    void markAnnouncementRead(Long groupId, Long userId);

    void setNotifyMuted(Long groupId, Long userId, boolean muted);

    void updateGroup(Long groupId, Long operatorId, String name, String announcement);

    List<Map<String, Object>> myGroups(Long userId);

    ChatGroupMessageDO sendGroupMessage(Long groupId, Long senderId, String content, Integer msgType, List<Long> mentionIds);

    ChatGroupMessageDO recallGroupMessage(Long groupId, Long operatorId, Long messageId);

    Page<ChatGroupMessageDO> groupMessages(Long groupId, Long userId, int pageNo, int pageSize);

    List<Map<String, Object>> groupMembers(Long groupId);

    List<ChatGroupLogVO> groupLogs(Long groupId, Long userId);

    void addMembers(Long groupId, List<Long> userIds, Long operatorId);

    void removeMember(Long groupId, Long memberUserId, Long operatorId);

    void setAdmin(Long groupId, Long memberUserId, Long operatorId, boolean admin);

    void setMuted(Long groupId, Long memberUserId, Long operatorId, boolean muted);

    void transferOwner(Long groupId, Long newOwnerId, Long operatorId);

    void quitGroup(Long groupId, Long userId);

    void dissolveGroup(Long groupId, Long operatorId);
}
