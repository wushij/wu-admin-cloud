package com.wu.cloud.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.ApprovalNodeDO;
import com.wu.cloud.ticket.entity.ApprovalRecordDO;
import com.wu.cloud.ticket.entity.ApprovalTaskDO;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.mapper.ApprovalFormMapper;
import com.wu.cloud.ticket.mapper.ApprovalNodeMapper;
import com.wu.cloud.ticket.mapper.ApprovalRecordMapper;
import com.wu.cloud.ticket.mapper.ApprovalTaskMapper;
import com.wu.cloud.ticket.mapper.NoticeMapper;
import com.wu.cloud.ticket.service.ApprovalFormService;
import com.wu.cloud.ticket.service.UserNameResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批单 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，
 * 与最新业务规范保持一致：直接注入 Mapper 承载数据访问）
 */
@Service
public class ApprovalFormServiceImpl implements ApprovalFormService {

    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private UserNameResolver userNameResolver;
    @Resource
    private RemoteUserService remoteUserService;
    @Resource
    private ApprovalRecordMapper recordMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private ApprovalNodeMapper nodeMapper;
    @Resource
    private ApprovalTaskMapper taskMapper;

    private String generateFormNo() {
        return "AP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @Override
    public PageResult<ApprovalFormDO> page(PageParam pageParam, String title, String formType, String status, Long applicantUserId) {
        Page<ApprovalFormDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<ApprovalFormDO> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            wrapper.like(ApprovalFormDO::getTitle, title);
        }
        if (formType != null && !formType.isBlank()) {
            wrapper.eq(ApprovalFormDO::getFormType, formType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ApprovalFormDO::getStatus, status);
        }
        if (applicantUserId != null) {
            wrapper.eq(ApprovalFormDO::getApplicantUserId, applicantUserId);
        }
        wrapper.orderByDesc(ApprovalFormDO::getUpdateTime);
        Page<ApprovalFormDO> result = approvalFormMapper.selectPage(page, wrapper);
        userNameResolver.fillNames(result.getRecords(), ApprovalFormDO::getApplicantUserId, ApprovalFormDO::setApplicantName);
        userNameResolver.fillNames(result.getRecords(), ApprovalFormDO::getApproverUserId, ApprovalFormDO::setApproverName);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public ApprovalFormDO getDetail(Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        form.setApplicantName(userNameResolver.resolve(form.getApplicantUserId()));
        form.setApproverName(userNameResolver.resolve(form.getApproverUserId()));
        return form;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApprovalFormDO form) {
        Long userId = UserContextHolder.getUserId();
        form.setApplicantUserId(userId);
        form.setCreator(userId != null ? userId.toString() : null);
        form.setFormNo(generateFormNo());
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("SUBMITTED");
        }
        approvalFormMapper.insert(form);
        return form.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ApprovalFormDO form) {
        form.setApplicantUserId(null);
        form.setCreator(null);
        approvalFormMapper.updateById(form);
    }

    private void record(Long formId, String action, String remark) {
        Long userId = UserContextHolder.getUserId();
        ApprovalRecordDO record = new ApprovalRecordDO();
        record.setFormId(formId);
        record.setOperatorUserId(userId);
        record.setAction(action);
        record.setRemark(remark);
        record.setCreator(userId != null ? userId.toString() : null);
        recordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String remark) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        // 权限校验：超管或拥有 approval:approve 权限可审批；否则仅审批单申请人本人不可自审
        Long currentUserId = UserContextHolder.getUserId();
        if (!PermissionChecker.isSuperAdmin()
                && !PermissionChecker.hasPermission("approval:approve")) {
            throw new BusinessException(403, "无审批权限");
        }
        if (currentUserId != null && currentUserId.equals(form.getApplicantUserId())) {
            throw new BusinessException(403, "不可审批本人提交的审批单");
        }
        Long approver = UserContextHolder.getUserId();
        approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getId, id)
                .set(ApprovalFormDO::getStatus, "APPROVED")
                .set(ApprovalFormDO::getApproverUserId, approver));
        record(id, "APPROVE", remark);
        NoticeDO notice = new NoticeDO();
        notice.setUserId(form.getApplicantUserId());
        notice.setTitle("审批结果通知");
        notice.setContent("您的审批单【" + form.getTitle() + "】已通过");
        notice.setBizType("APPROVAL");
        notice.setBizId(id);
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String remark) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getId, id).set(ApprovalFormDO::getStatus, "REJECTED"));
        record(id, "REJECT", remark);
        NoticeDO notice = new NoticeDO();
        notice.setUserId(form.getApplicantUserId());
        notice.setTitle("审批结果通知");
        notice.setContent("您的审批单【" + form.getTitle() + "】已被驳回");
        notice.setBizType("APPROVAL");
        notice.setBizId(id);
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getId, id).set(ApprovalFormDO::getStatus, "ARCHIVED"));
        record(id, "ARCHIVE", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            return;
        }
        // 权限校验：非超管仅可删除本人提交的审批单
        Long currentUserId = UserContextHolder.getUserId();
        if (!PermissionChecker.isSuperAdmin() && currentUserId != null
                && !currentUserId.equals(form.getApplicantUserId())) {
            throw new BusinessException(403, "无权删除他人审批单");
        }
        // 级联清理：节点 + 任务（按节点关联） + 记录
        List<ApprovalNodeDO> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNodeDO>().eq(ApprovalNodeDO::getFormId, id));
        if (!nodes.isEmpty()) {
            List<Long> nodeIds = nodes.stream().map(ApprovalNodeDO::getId).collect(Collectors.toList());
            taskMapper.delete(new LambdaQueryWrapper<ApprovalTaskDO>().in(ApprovalTaskDO::getNodeId, nodeIds));
        }
        nodeMapper.delete(new LambdaQueryWrapper<ApprovalNodeDO>().eq(ApprovalNodeDO::getFormId, id));
        recordMapper.delete(new LambdaQueryWrapper<ApprovalRecordDO>().eq(ApprovalRecordDO::getFormId, id));
        approvalFormMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = approvalFormMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站审批单不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = approvalFormMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站审批单不存在");
        }
        recordMapper.delete(new LambdaQueryWrapper<ApprovalRecordDO>().eq(ApprovalRecordDO::getFormId, id));
    }

    @Override
    public PageResult<ApprovalFormDO> recyclePage(PageParam pageParam, String title, String formType, String status) {
        Page<ApprovalFormDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<ApprovalFormDO> result = (Page<ApprovalFormDO>) approvalFormMapper.selectDeletedPage(page, title, formType, status != null ? status : "");
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredRecycleBin() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        approvalFormMapper.selectDeletedPage(new Page<>(1, 500), null, null, null).getRecords().stream()
                .filter(f -> f.getUpdateTime() != null && f.getUpdateTime().isBefore(cutoff))
                .forEach(f -> deletePermanent(f.getId()));
    }

    @Override
    public List<ApprovalRecordDO> recordList(Long formId) {
        List<ApprovalRecordDO> list = recordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecordDO>()
                        .eq(ApprovalRecordDO::getFormId, formId)
                        .orderByAsc(ApprovalRecordDO::getCreateTime));
        userNameResolver.fillNames(list, ApprovalRecordDO::getOperatorUserId, ApprovalRecordDO::setOperatorName);
        return list;
    }

    @Override
    public List<Map<String, Object>> listApproverOptions() {
        // 启用状态用户全量候选（经用户服务 RPC），排除当前登录用户；名称直接取自用户档案
        Long currentUserId = UserContextHolder.getUserId();
        R<List<SysUserDTO>> resp = remoteUserService.listActiveUsers(currentUserId);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new BusinessException("用户服务暂不可用，审批人候选列表获取失败，请稍后重试");
        }
        List<Map<String, Object>> options = new ArrayList<>(resp.getData().size());
        for (SysUserDTO user : resp.getData()) {
            if (user == null || user.getUserId() == null) {
                continue;
            }
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("userId", user.getUserId());
            opt.put("username", user.getUsername());
            opt.put("nickname", user.getNickname());
            options.add(opt);
        }
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        Long userId = UserContextHolder.getUserId();
        if (form.getApplicantUserId() == null || !form.getApplicantUserId().equals(userId)) {
            throw new BusinessException(403, "仅申请人可撤回");
        }
        if (!"SUBMITTED".equals(form.getStatus())) {
            throw new BusinessException("仅待审批状态可撤回");
        }
        approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getId, id)
                .set(ApprovalFormDO::getStatus, "WITHDRAWN"));
        record(id, "WITHDRAW", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitWithNodes(Long formId, List<Long> approverIds, List<String> nodeNames) {
        ApprovalFormDO form = approvalFormMapper.selectById(formId);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        if (nodeNames == null || nodeNames.isEmpty()) {
            throw new BusinessException("节点名列表不能为空");
        }
        if (approverIds == null || approverIds.isEmpty()) {
            throw new BusinessException("审批人列表不能为空");
        }
        // 清理旧节点（允许重新提交初始化）
        List<ApprovalNodeDO> oldNodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ApprovalNodeDO>().eq(ApprovalNodeDO::getFormId, formId));
        if (!oldNodes.isEmpty()) {
            List<Long> oldNodeIds = oldNodes.stream().map(ApprovalNodeDO::getId).collect(Collectors.toList());
            taskMapper.delete(new LambdaQueryWrapper<ApprovalTaskDO>().in(ApprovalTaskDO::getNodeId, oldNodeIds));
            nodeMapper.delete(new LambdaQueryWrapper<ApprovalNodeDO>().eq(ApprovalNodeDO::getFormId, formId));
        }
        String approverIdsStr = approverIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Long userId = UserContextHolder.getUserId();
        int order = 1;
        for (String nodeName : nodeNames) {
            ApprovalNodeDO node = new ApprovalNodeDO();
            node.setFormId(formId);
            node.setNodeOrder(order);
            node.setNodeName(nodeName);
            node.setApproverUserIds(approverIdsStr);
            node.setStrategy("ALL");
            node.setStatus("PENDING");
            node.setCreator(userId != null ? userId.toString() : null);
            nodeMapper.insert(node);
            // 为每个审批人创建会签任务（B3）
            for (Long approverId : approverIds) {
                ApprovalTaskDO task = new ApprovalTaskDO();
                task.setNodeId(node.getId());
                task.setApproverUserId(approverId);
                task.setStatus("PENDING");
                task.setCreator(userId != null ? userId.toString() : null);
                taskMapper.insert(task);
            }
            order++;
        }
        // 审批单状态置为审批中
        approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getId, formId)
                .set(ApprovalFormDO::getStatus, "SUBMITTED"));
        record(formId, "SUBMIT", "提交多节点审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveNode(Long formId, Long nodeId, String remark) {
        ApprovalFormDO form = approvalFormMapper.selectById(formId);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        ApprovalNodeDO node = nodeMapper.selectById(nodeId);
        if (node == null || !formId.equals(node.getFormId())) {
            throw new BusinessException(404, "审批节点不存在");
        }
        if (!"PENDING".equals(node.getStatus())) {
            throw new BusinessException("当前节点已处理");
        }
        // 校验为当前激活节点（最小顺序的 PENDING 节点）
        ApprovalNodeDO currentNode = nodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalNodeDO>()
                        .eq(ApprovalNodeDO::getFormId, formId)
                        .eq(ApprovalNodeDO::getStatus, "PENDING")
                        .orderByAsc(ApprovalNodeDO::getNodeOrder)
                        .last("LIMIT 1"));
        if (currentNode == null || !currentNode.getId().equals(nodeId)) {
            throw new BusinessException("请按顺序审批当前节点");
        }
        Long userId = UserContextHolder.getUserId();
        // 找到当前用户的会签任务
        ApprovalTaskDO task = taskMapper.selectOne(
                new LambdaQueryWrapper<ApprovalTaskDO>()
                        .eq(ApprovalTaskDO::getNodeId, nodeId)
                        .eq(ApprovalTaskDO::getApproverUserId, userId));
        if (task == null) {
            throw new BusinessException(403, "您不是该节点的审批人");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("您已审批过该节点");
        }
        taskMapper.update(null, new LambdaUpdateWrapper<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getId, task.getId())
                .set(ApprovalTaskDO::getStatus, "APPROVED")
                .set(ApprovalTaskDO::getRemark, remark));
        record(formId, "APPROVE", "节点[" + node.getNodeName() + "]审批通过:" + (remark == null ? "" : remark));

        // 会签策略判定（B3）
        List<ApprovalTaskDO> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<ApprovalTaskDO>().eq(ApprovalTaskDO::getNodeId, nodeId));
        String strategy = node.getStrategy() == null ? "ALL" : node.getStrategy();
        boolean nodeDone;
        if ("ANY".equals(strategy)) {
            nodeDone = tasks.stream().anyMatch(t -> "APPROVED".equals(t.getStatus()));
        } else {
            // ALL：全部通过才完成
            nodeDone = tasks.stream().allMatch(t -> "APPROVED".equals(t.getStatus()));
        }
        if (!nodeDone) {
            return;
        }
        // 节点完成，置为 APPROVED
        nodeMapper.update(null, new LambdaUpdateWrapper<ApprovalNodeDO>()
                .eq(ApprovalNodeDO::getId, nodeId)
                .set(ApprovalNodeDO::getStatus, "APPROVED"));
        // 查找下一节点（已是 PENDING，自然激活）
        ApprovalNodeDO nextNode = nodeMapper.selectOne(
                new LambdaQueryWrapper<ApprovalNodeDO>()
                        .eq(ApprovalNodeDO::getFormId, formId)
                        .gt(ApprovalNodeDO::getNodeOrder, node.getNodeOrder())
                        .orderByAsc(ApprovalNodeDO::getNodeOrder)
                        .last("LIMIT 1"));
        if (nextNode == null) {
            // 全部节点通过，审批单 APPROVED
            approvalFormMapper.update(null, new LambdaUpdateWrapper<ApprovalFormDO>()
                    .eq(ApprovalFormDO::getId, formId)
                    .set(ApprovalFormDO::getStatus, "APPROVED"));
        }
    }
}
