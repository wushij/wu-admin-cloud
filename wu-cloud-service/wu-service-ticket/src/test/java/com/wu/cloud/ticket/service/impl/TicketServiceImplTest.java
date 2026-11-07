package com.wu.cloud.ticket.service.impl;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.ticket.entity.TicketDO;
import com.wu.cloud.ticket.mapper.TicketMapper;
import com.wu.cloud.ticket.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import static com.wu.cloud.ticket.testsupport.MybatisMockMatchers.anyLambdaUpdateWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TicketServiceImpl 单元测试")
class TicketServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private com.wu.cloud.ticket.service.UserNameResolver userNameResolver;
    @Mock
    private com.wu.cloud.ticket.mapper.TicketCommentMapper commentMapper;
    @Mock
    private com.wu.cloud.ticket.mapper.NoticeMapper noticeMapper;
    @Mock
    private com.wu.cloud.ticket.mapper.TicketAttachmentMapper attachmentMapper;
    @Mock
    private com.wu.cloud.api.system.feign.RemoteUserService remoteUserService;
    @Mock
    private com.wu.cloud.api.file.feign.RemoteFileService remoteFileService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder().userId(1L).username("alice").build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("create：写入创建人与默认 OPEN 状态")
    void create_setsCreatorAndDefaultStatus() {
        when(ticketMapper.insert(any(TicketDO.class))).thenAnswer(inv -> {
            TicketDO t = inv.getArgument(0);
            t.setId(100L);
            return 1;
        });

        TicketDO ticket = new TicketDO();
        ticket.setTitle("打印机故障");
        Long id = ticketService.create(ticket);

        assertEquals(100L, id);
        assertEquals(1L, ticket.getCreatorUserId());
        assertEquals("OPEN", ticket.getStatus());
        assertNotNull(ticket.getTicketNo());
        assertTrue(ticket.getTicketNo().startsWith("TK"));
    }

    @Test
    @DisplayName("changeStatus：工单不存在拒绝")
    void changeStatus_missing_throws() {
        when(ticketMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.changeStatus(9L, "CLOSED"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("changeStatus：非法状态拒绝")
    void changeStatus_illegalStatus_throws() {
        TicketDO ticket = new TicketDO();
        ticket.setId(9L);
        ticket.setCreatorUserId(1L);
        when(ticketMapper.selectById(9L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.changeStatus(9L, "DONE"));
        assertEquals("非法状态", ex.getMessage());
    }

    @Test
    @DisplayName("changeStatus：非创建人/非受理人拒绝")
    void changeStatus_otherUser_forbidden() {
        TicketDO ticket = new TicketDO();
        ticket.setId(9L);
        ticket.setCreatorUserId(2L);
        ticket.setAssigneeUserId(3L);
        when(ticketMapper.selectById(9L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.changeStatus(9L, "IN_PROGRESS"));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("changeStatus：创建人可流转并更新状态")
    void changeStatus_creator_ok() {
        TicketDO ticket = new TicketDO();
        ticket.setId(9L);
        ticket.setCreatorUserId(1L);
        when(ticketMapper.selectById(9L)).thenReturn(ticket);
        when(ticketMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        assertDoesNotThrow(() -> ticketService.changeStatus(9L, "RESOLVED"));
        verify(ticketMapper).update(isNull(), anyLambdaUpdateWrapper());
    }

    @Test
    @DisplayName("changeStatus：超管可流转他人工单")
    void changeStatus_superAdmin_ok() {
        UserContextHolder.set(UserContext.builder()
                .userId(99L)
                .roles(Set.of(PermissionChecker.SUPER_ADMIN_ROLE))
                .build());
        TicketDO ticket = new TicketDO();
        ticket.setId(9L);
        ticket.setCreatorUserId(1L);
        when(ticketMapper.selectById(9L)).thenReturn(ticket);
        when(ticketMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        assertDoesNotThrow(() -> ticketService.changeStatus(9L, "CLOSED"));
        verify(ticketMapper).update(eq(null), anyLambdaUpdateWrapper());
    }

    @Test
    @DisplayName("addComment：工单不存在拒绝")
    void addComment_missing_throws() {
        when(ticketMapper.selectById(5L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.addComment(5L, "跟进"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("addComment：成功写入评论")
    void addComment_ok() {
        TicketDO ticket = new TicketDO();
        ticket.setId(5L);
        when(ticketMapper.selectById(5L)).thenReturn(ticket);
        doAnswer(inv -> {
            com.wu.cloud.ticket.entity.TicketCommentDO c = inv.getArgument(0);
            c.setId(77L);
            return 1;
        }).when(commentMapper).insert(any(com.wu.cloud.ticket.entity.TicketCommentDO.class));

        assertEquals(77L, ticketService.addComment(5L, "已处理"));
        verify(commentMapper).insert(argThat((com.wu.cloud.ticket.entity.TicketCommentDO c) ->
                Long.valueOf(5L).equals(c.getTicketId())
                        && Long.valueOf(1L).equals(c.getUserId())
                        && "已处理".equals(c.getContent())));
    }

    @Test
    @DisplayName("delete：无权删除他人工单")
    void delete_otherUser_forbidden() {
        TicketDO ticket = new TicketDO();
        ticket.setId(3L);
        ticket.setCreatorUserId(2L);
        when(ticketMapper.selectById(3L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class, () -> ticketService.delete(3L));
        assertEquals(403, ex.getCode());
        verify(ticketMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("assign：更新受理人并通知")
    void assign_updatesAndNotifies() {
        when(ticketMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);
        doReturn(1).when(noticeMapper).insert(any(com.wu.cloud.ticket.entity.NoticeDO.class));

        ticketService.assign(9L, 8L);

        verify(ticketMapper).update(isNull(), anyLambdaUpdateWrapper());
        verify(noticeMapper).insert(argThat((com.wu.cloud.ticket.entity.NoticeDO n) ->
                Long.valueOf(8L).equals(n.getUserId()) && "TICKET".equals(n.getBizType())));
    }
}
