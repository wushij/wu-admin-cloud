package com.wu.cloud.ticket.service.impl;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.ApprovalNodeDO;
import com.wu.cloud.ticket.entity.ApprovalRecordDO;
import com.wu.cloud.ticket.entity.ApprovalTaskDO;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.mapper.ApprovalFormMapper;
import com.wu.cloud.ticket.mapper.ApprovalRecordMapper;
import com.wu.cloud.ticket.mapper.NoticeMapper;
import com.wu.cloud.ticket.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static com.wu.cloud.ticket.testsupport.MybatisMockMatchers.anyLambdaUpdateWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApprovalFormServiceImpl 单元测试")
class ApprovalFormServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private ApprovalFormMapper approvalFormMapper;
    @Mock
    private com.wu.cloud.ticket.service.UserNameResolver userNameResolver;
    @Mock
    private com.wu.cloud.api.system.feign.RemoteUserService remoteUserService;
    @Mock
    private ApprovalRecordMapper recordMapper;
    @Mock
    private NoticeMapper noticeMapper;
    @Mock
    private com.wu.cloud.ticket.mapper.ApprovalNodeMapper nodeMapper;
    @Mock
    private com.wu.cloud.ticket.mapper.ApprovalTaskMapper taskMapper;

    @InjectMocks
    private ApprovalFormServiceImpl approvalFormService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder()
                .userId(2L)
                .permissions(Set.of("approval:approve"))
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("approve：审批单不存在拒绝")
    void approve_missing_throws() {
        when(approvalFormMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.approve(1L, "ok"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("approve：无审批权限拒绝")
    void approve_noPermission_throws() {
        UserContextHolder.set(UserContext.builder().userId(2L).build());
        ApprovalFormDO form = form(1L, 10L);
        when(approvalFormMapper.selectById(1L)).thenReturn(form);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.approve(1L, "ok"));
        assertEquals(403, ex.getCode());
        assertEquals("无审批权限", ex.getMessage());
    }

    @Test
    @DisplayName("approve：不可自审")
    void approve_self_forbidden() {
        UserContextHolder.set(UserContext.builder()
                .userId(10L)
                .permissions(Set.of("approval:approve"))
                .build());
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.approve(1L, "ok"));
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("不可审批本人"));
    }

    @Test
    @DisplayName("approve：成功更新状态并通知申请人")
    void approve_success() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        when(approvalFormMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);
        doReturn(1).when(recordMapper).insert(any(ApprovalRecordDO.class));
        doReturn(1).when(noticeMapper).insert(any(NoticeDO.class));

        approvalFormService.approve(1L, "同意");

        verify(approvalFormMapper).update(isNull(), anyLambdaUpdateWrapper());
        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertEquals("APPROVE", recordCaptor.getValue().getAction());
        ArgumentCaptor<NoticeDO> noticeCaptor = ArgumentCaptor.forClass(NoticeDO.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertEquals(10L, noticeCaptor.getValue().getUserId());
        assertEquals("APPROVAL", noticeCaptor.getValue().getBizType());
    }

    @Test
    @DisplayName("approve：超管可审批")
    void approve_superAdmin_ok() {
        UserContextHolder.set(UserContext.builder()
                .userId(99L)
                .roles(Set.of(PermissionChecker.SUPER_ADMIN_ROLE))
                .build());
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        when(approvalFormMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);
        doReturn(1).when(recordMapper).insert(any(ApprovalRecordDO.class));
        doReturn(1).when(noticeMapper).insert(any(NoticeDO.class));

        assertDoesNotThrow(() -> approvalFormService.approve(1L, null));
        verify(recordMapper).insert(any(ApprovalRecordDO.class));
    }

    @Test
    @DisplayName("reject：成功驳回并通知")
    void reject_success() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        when(approvalFormMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);
        doReturn(1).when(recordMapper).insert(any(ApprovalRecordDO.class));
        doReturn(1).when(noticeMapper).insert(any(NoticeDO.class));

        approvalFormService.reject(1L, "资料不全");

        ArgumentCaptor<ApprovalRecordDO> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordDO.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertEquals("REJECT", recordCaptor.getValue().getAction());
        ArgumentCaptor<NoticeDO> noticeCaptor = ArgumentCaptor.forClass(NoticeDO.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertTrue(noticeCaptor.getValue().getContent().contains("驳回"));
    }

    @Test
    @DisplayName("reject：审批单不存在拒绝")
    void reject_missing_throws() {
        when(approvalFormMapper.selectById(8L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.reject(8L, "x"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("delete：无权删除他人审批单")
    void delete_otherUser_forbidden() {
        ApprovalFormDO form = form(1L, 10L);
        when(approvalFormMapper.selectById(1L)).thenReturn(form);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> approvalFormService.delete(1L));
        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("无权删除"));
    }

    @Test
    @DisplayName("delete：申请人可删除并级联清理")
    void delete_applicant_ok() {
        UserContextHolder.set(UserContext.builder().userId(10L).build());
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        when(nodeMapper.selectList(any())).thenReturn(List.of());
        when(recordMapper.delete(any())).thenReturn(1);
        when(approvalFormMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> approvalFormService.delete(1L));
        verify(recordMapper).delete(any());
        verify(approvalFormMapper).deleteById(1L);
    }

    @Test
    @DisplayName("submitWithNodes：空节点/空审批人拒绝")
    void submitWithNodes_validation() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        assertThrows(BusinessException.class,
                () -> approvalFormService.submitWithNodes(1L, List.of(2L), List.of()));
        assertThrows(BusinessException.class,
                () -> approvalFormService.submitWithNodes(1L, List.of(), List.of("一级审批")));
    }

    @Test
    @DisplayName("submitWithNodes：清理旧节点并创建会签任务")
    void submitWithNodes_createsNodesAndTasks() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        when(nodeMapper.selectList(any())).thenReturn(List.of());
        doAnswer(inv -> {
            ApprovalNodeDO n = inv.getArgument(0);
            n.setId(100L);
            return 1;
        }).when(nodeMapper).insert(any(ApprovalNodeDO.class));
        doReturn(1).when(taskMapper).insert(any(ApprovalTaskDO.class));
        doReturn(1).when(recordMapper).insert(any(ApprovalRecordDO.class));
        when(approvalFormMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        approvalFormService.submitWithNodes(1L, List.of(2L, 3L), List.of("一级", "二级"));

        verify(nodeMapper, times(2)).insert(any(ApprovalNodeDO.class));
        verify(taskMapper, times(4)).insert(any(ApprovalTaskDO.class));
        verify(approvalFormMapper).update(isNull(), anyLambdaUpdateWrapper());
        verify(recordMapper).insert(any(ApprovalRecordDO.class));
    }

    @Test
    @DisplayName("approveNode：非当前节点 / 非审批人拒绝")
    void approveNode_guards() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        ApprovalNodeDO node = node(50L, 1L, 1, "PENDING", "ALL");
        when(nodeMapper.selectById(50L)).thenReturn(node);
        ApprovalNodeDO current = node(51L, 1L, 1, "PENDING", "ALL");
        when(nodeMapper.selectOne(any())).thenReturn(current);

        BusinessException orderEx = assertThrows(BusinessException.class,
                () -> approvalFormService.approveNode(1L, 50L, "ok"));
        assertTrue(orderEx.getMessage().contains("按顺序审批"));

        when(nodeMapper.selectOne(any())).thenReturn(node);
        when(taskMapper.selectOne(any())).thenReturn(null);
        BusinessException permEx = assertThrows(BusinessException.class,
                () -> approvalFormService.approveNode(1L, 50L, "ok"));
        assertEquals(403, permEx.getCode());
    }

    @Test
    @DisplayName("approveNode：ALL 策略全员通过后完结表单")
    void approveNode_allStrategy_completesForm() {
        when(approvalFormMapper.selectById(1L)).thenReturn(form(1L, 10L));
        ApprovalNodeDO node = node(50L, 1L, 1, "PENDING", "ALL");
        when(nodeMapper.selectById(50L)).thenReturn(node);

        java.util.concurrent.atomic.AtomicInteger selectOneCalls = new java.util.concurrent.atomic.AtomicInteger();
        when(nodeMapper.selectOne(any())).thenAnswer(inv ->
                selectOneCalls.getAndIncrement() == 0 ? node : null);

        ApprovalTaskDO myTask = task(9L, 50L, 2L, "PENDING");
        when(taskMapper.selectOne(any())).thenReturn(myTask);
        when(taskMapper.update(isNull(), any())).thenReturn(1);
        doReturn(1).when(recordMapper).insert(any(ApprovalRecordDO.class));

        ApprovalTaskDO approved = task(9L, 50L, 2L, "APPROVED");
        when(taskMapper.selectList(any())).thenReturn(List.of(approved));
        when(nodeMapper.update(isNull(), any())).thenReturn(1);
        when(approvalFormMapper.update(isNull(), anyLambdaUpdateWrapper())).thenReturn(1);

        assertDoesNotThrow(() -> approvalFormService.approveNode(1L, 50L, "同意"));
        verify(taskMapper).update(isNull(), any());
        verify(nodeMapper).update(isNull(), any());
        verify(approvalFormMapper).update(isNull(), anyLambdaUpdateWrapper());
    }

    private static ApprovalFormDO form(Long id, Long applicantUserId) {
        ApprovalFormDO form = new ApprovalFormDO();
        form.setId(id);
        form.setApplicantUserId(applicantUserId);
        form.setTitle("请假申请");
        form.setStatus("SUBMITTED");
        return form;
    }

    private static ApprovalNodeDO node(
            Long id, Long formId, int order, String status, String strategy) {
        ApprovalNodeDO n = new ApprovalNodeDO();
        n.setId(id);
        n.setFormId(formId);
        n.setNodeOrder(order);
        n.setNodeName("节点" + order);
        n.setStatus(status);
        n.setStrategy(strategy);
        return n;
    }

    private static ApprovalTaskDO task(
            Long id, Long nodeId, Long approverId, String status) {
        ApprovalTaskDO t = new ApprovalTaskDO();
        t.setId(id);
        t.setNodeId(nodeId);
        t.setApproverUserId(approverId);
        t.setStatus(status);
        return t;
    }
}
