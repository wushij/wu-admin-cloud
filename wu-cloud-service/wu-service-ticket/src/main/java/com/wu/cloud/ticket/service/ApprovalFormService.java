package com.wu.cloud.ticket.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.PageResult;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.ApprovalRecordDO;

import java.util.List;
import java.util.Map;

/**
 * 审批单 Service
 */
public interface ApprovalFormService {

    PageResult<ApprovalFormDO> page(PageParam pageParam, String title, String formType, String status, Long applicantUserId);

    ApprovalFormDO getDetail(Long id);

    Long create(ApprovalFormDO form);

    void update(ApprovalFormDO form);

    void approve(Long id, String remark);

    void reject(Long id, String remark);

    void archive(Long id);

    void delete(Long id);

    void restore(Long id);

    void deletePermanent(Long id);

    PageResult<ApprovalFormDO> recyclePage(PageParam pageParam, String title, String formType, String status);

    void purgeExpiredRecycleBin();

    /** B1: 审批记录列表（按 createTime 升序，填充 operatorName） */
    List<ApprovalRecordDO> recordList(Long formId);

    /** B1: 审批人下拉选项（微服务简化：从最近审批记录提取 operator 列表） */
    List<Map<String, Object>> listApproverOptions();

    /** B4: 审批撤回（校验当前用户为申请人且状态为 SUBMITTED，改为 WITHDRAWN） */
    void withdraw(Long id);

    /** B2: 提交并初始化多节点（传入审批人ID列表与节点名列表，自动创建节点与会签任务） */
    void submitWithNodes(Long formId, List<Long> approverIds, List<String> nodeNames);

    /** B2/B3: 审批当前节点（会签：ALL 全通过 / ANY 任一通过 才流转） */
    void approveNode(Long formId, Long nodeId, String remark);
}
