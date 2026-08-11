package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批节点定义（多节点线性流转）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_node")
public class ApprovalNodeDO extends BaseEntity {

    private Long formId;
    /** 节点顺序 1,2,3... */
    private Integer nodeOrder;
    private String nodeName;
    /** 逗号分隔多审批人ID */
    private String approverUserIds;
    /** ALL=全通过 / ANY=任一通过 */
    private String strategy;
    /** PENDING/APPROVED/REJECTED */
    private String status;
}
