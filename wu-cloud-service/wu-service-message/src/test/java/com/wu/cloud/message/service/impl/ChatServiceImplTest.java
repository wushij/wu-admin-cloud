package com.wu.cloud.message.service.impl;

import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.message.entity.ChatMessageDO;
import com.wu.cloud.message.entity.UserBlacklistDO;
import com.wu.cloud.message.mapper.ChatGroupLogMapper;
import com.wu.cloud.message.mapper.ChatGroupMapper;
import com.wu.cloud.message.mapper.ChatGroupMemberMapper;
import com.wu.cloud.message.mapper.ChatGroupMessageMapper;
import com.wu.cloud.message.mapper.ChatMessageMapper;
import com.wu.cloud.message.mapper.UserBlacklistMapper;
import com.wu.cloud.message.testsupport.MybatisLambdaTestBase;
import com.wu.cloud.message.testsupport.MybatisMockMatchers;
import com.wu.cloud.message.vo.ChatPeerLatestVO;
import com.wu.cloud.message.vo.ChatPeerUnreadVO;
import com.wu.cloud.message.websocket.MessageWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatServiceImpl 单元测试（与既有规范对齐 ChatServiceTest）")
class ChatServiceImplTest extends MybatisLambdaTestBase {

    private static final long ME = 1L;

    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private UserBlacklistMapper blacklistMapper;
    @Mock
    private ChatGroupMapper chatGroupMapper;
    @Mock
    private ChatGroupMemberMapper groupMemberMapper;
    @Mock
    private ChatGroupMessageMapper groupMessageMapper;
    @Mock
    private ChatGroupLogMapper groupLogMapper;
    @Mock
    private MessageWebSocketHandler webSocketHandler;
    @Mock
    private RemoteUserService remoteUserService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    @DisplayName("listUsers：无启用用户时返回空列表")
    void listUsers_emptyWhenNoPeers() {
        when(remoteUserService.listActiveUsers(ME))
                .thenReturn(R.ok(List.of()));

        assertTrue(chatService.listUsers(ME).isEmpty());
        verifyNoInteractions(chatMessageMapper, blacklistMapper);
    }

    @Test
    @DisplayName("listUsers：批量查询黑名单/最新消息/未读数")
    void listUsers_usesBatchQueries() {
        SysUserDTO peer = user(2L, "bob", "鲍勃");
        when(remoteUserService.listActiveUsers(ME))
                .thenReturn(R.ok(List.of(peer)));
        when(blacklistMapper.selectBlockedUserIds(ME)).thenReturn(List.of(2L));

        ChatPeerLatestVO latest = new ChatPeerLatestVO();
        latest.setPeerId(2L);
        latest.setContent("hi");
        latest.setMsgType(1);
        latest.setSendTime(LocalDateTime.now().minusMinutes(1));
        when(chatMessageMapper.selectLatestPerPeer(ME)).thenReturn(List.of(latest));

        ChatPeerUnreadVO unread = new ChatPeerUnreadVO();
        unread.setSenderId(2L);
        unread.setUnreadCount(3L);
        when(chatMessageMapper.selectUnreadCountPerSender(ME)).thenReturn(List.of(unread));
        when(webSocketHandler.isOnline(2L)).thenReturn(true);

        List<Map<String, Object>> result = chatService.listUsers(ME);

        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertEquals(2L, item.get("id"));
        assertEquals(true, item.get("isBlocked"));
        assertEquals(3L, item.get("unreadCount"));
        assertNotNull(item.get("lastMessage"));

        verify(blacklistMapper, times(1)).selectBlockedUserIds(ME);
        verify(chatMessageMapper, times(1)).selectLatestPerPeer(ME);
        verify(chatMessageMapper, times(1)).selectUnreadCountPerSender(ME);
    }

    @Test
    @DisplayName("sendPrivate：存在拉黑关系时拒绝发送")
    void sendPrivate_blocked() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendPrivate(ME, 2L, "hi", 1));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("拉黑"));
        verify(chatMessageMapper, never()).insert(any(ChatMessageDO.class));
    }

    @Test
    @DisplayName("sendPrivate：成功发送并推送 WebSocket")
    void sendPrivate_success() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);
        when(remoteUserService.getUserById(ME))
                .thenReturn(R.ok(user(ME, "alice", "爱丽丝")));
        doAnswer(inv -> {
            ChatMessageDO msg = inv.getArgument(0);
            msg.setId(200L);
            return 1;
        }).when(chatMessageMapper).insert(any(ChatMessageDO.class));

        ChatMessageDO saved = chatService.sendPrivate(ME, 2L, "hello", 1);

        assertEquals(200L, saved.getId());
        assertEquals(ME, saved.getSenderId());
        assertEquals(2L, saved.getReceiverId());
        assertEquals("hello", saved.getContent());
        assertEquals("爱丽丝", saved.getSenderName());
        verify(chatMessageMapper).insert(any(ChatMessageDO.class));
        verify(webSocketHandler).sendChatPayload(eq(2L), anyMap());
        verify(webSocketHandler).sendTypingStopPayload(2L, ME);
    }

    @Test
    @DisplayName("block：未拉黑时插入记录")
    void block_insertsWhenNotBlocked() {
        when(blacklistMapper.selectCount(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(0L);

        chatService.block(ME, 2L);

        verify(blacklistMapper).insert(any(UserBlacklistDO.class));
    }

    @Test
    @DisplayName("block：不能拉黑自己")
    void block_rejectSelf() {
        BusinessException ex = assertThrows(BusinessException.class, () -> chatService.block(ME, ME));
        assertEquals(400, ex.getCode());
        verify(blacklistMapper, never()).insert(any(UserBlacklistDO.class));
    }

    @Test
    @DisplayName("recallPrivate：只能撤回自己的消息")
    void recallPrivate_notOwner_forbidden() {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setId(10L);
        msg.setSenderId(2L);
        msg.setSendTime(LocalDateTime.now());
        when(chatMessageMapper.selectById(10L)).thenReturn(msg);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.recallPrivate(ME, 10L));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("recallPrivate：成功删除并推送")
    void recallPrivate_ok() {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setId(10L);
        msg.setSenderId(ME);
        msg.setReceiverId(2L);
        msg.setSendTime(LocalDateTime.now());
        when(chatMessageMapper.selectById(10L)).thenReturn(msg);

        chatService.recallPrivate(ME, 10L);

        verify(chatMessageMapper).deleteById(10L);
        verify(webSocketHandler).sendChatPayload(eq(2L), argThat(p -> Boolean.TRUE.equals(p.get("recall"))));
    }

    @Test
    @DisplayName("createGroup：非超管拒绝")
    void createGroup_notAdmin_forbidden() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.createGroup(ME, "群", List.of(2L)));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("sendGroupMessage：非成员拒绝")
    void sendGroup_notMember_forbidden() {
        com.wu.cloud.message.entity.ChatGroupDO group = new com.wu.cloud.message.entity.ChatGroupDO();
        group.setId(5L);
        group.setStatus(1);
        when(chatGroupMapper.selectById(5L)).thenReturn(group);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendGroupMessage(5L, ME, "hi", 1, null));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("sendGroupMessage：禁言拒绝")
    void sendGroup_muted_forbidden() {
        com.wu.cloud.message.entity.ChatGroupDO group = new com.wu.cloud.message.entity.ChatGroupDO();
        group.setId(5L);
        group.setStatus(1);
        when(chatGroupMapper.selectById(5L)).thenReturn(group);
        com.wu.cloud.message.entity.ChatGroupMemberDO me = new com.wu.cloud.message.entity.ChatGroupMemberDO();
        me.setUserId(ME);
        me.setMuted(1);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(me);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.sendGroupMessage(5L, ME, "hi", 1, null));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("禁言"));
    }

    @Test
    @DisplayName("sendGroupMessage：成功入库并推送")
    void sendGroup_ok() {
        com.wu.cloud.message.entity.ChatGroupDO group = new com.wu.cloud.message.entity.ChatGroupDO();
        group.setId(5L);
        group.setStatus(1);
        when(chatGroupMapper.selectById(5L)).thenReturn(group);

        com.wu.cloud.message.entity.ChatGroupMemberDO me = new com.wu.cloud.message.entity.ChatGroupMemberDO();
        me.setUserId(ME);
        me.setMuted(0);
        when(groupMemberMapper.selectOne(MybatisMockMatchers.anyLambdaQueryWrapper())).thenReturn(me);
        when(remoteUserService.getUserById(ME))
                .thenReturn(R.ok(user(ME, "alice", "爱丽丝")));

        doAnswer(inv -> {
            com.wu.cloud.message.entity.ChatGroupMessageDO m = inv.getArgument(0);
            m.setId(300L);
            return 1;
        }).when(groupMessageMapper).insert(any(com.wu.cloud.message.entity.ChatGroupMessageDO.class));

        com.wu.cloud.message.entity.ChatGroupMemberDO peer = new com.wu.cloud.message.entity.ChatGroupMemberDO();
        peer.setUserId(2L);
        when(groupMemberMapper.selectList(MybatisMockMatchers.anyLambdaQueryWrapper()))
                .thenReturn(List.of(me, peer));

        var saved = chatService.sendGroupMessage(5L, ME, "群消息", 1, null);
        assertEquals(300L, saved.getId());
        verify(webSocketHandler).sendGroupChatPayload(eq(2L), anyMap());
    }

    @Test
    @DisplayName("dissolveGroup：非群主拒绝")
    void dissolveGroup_notOwner_forbidden() {
        com.wu.cloud.message.entity.ChatGroupDO group = new com.wu.cloud.message.entity.ChatGroupDO();
        group.setId(5L);
        group.setStatus(1);
        group.setOwnerId(2L);
        when(chatGroupMapper.selectById(5L)).thenReturn(group);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.dissolveGroup(5L, ME));
        assertEquals(403, ex.getCode());
    }

    private static SysUserDTO user(Long id, String username, String nickname) {
        SysUserDTO u = new SysUserDTO();
        u.setUserId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setAvatar("/avatar/" + id + ".png");
        return u;
    }
}
