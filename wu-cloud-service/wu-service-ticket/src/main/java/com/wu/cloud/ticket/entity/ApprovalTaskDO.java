package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批任务（会签粒度，每个审批人一条）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_task")
public class ApprovalTaskDO extends BaseEntity {

    private Long nodeId;
    private Long approverUserId;
    /** PENDING/APPROVED/REJECTED */
    private String status;
    private String remark;
}
