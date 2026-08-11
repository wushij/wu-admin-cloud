package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_form")
public class ApprovalFormDO extends BaseEntity {

    private String formNo;
    /** LEAVE/PURCHASE/REIMBURSE/SEAL/CONTRACT/GENERAL/REGISTER */
    private String formType;
    private String title;
    private String content;
    /** SUBMITTED/APPROVED/REJECTED/ARCHIVED/WITHDRAWN */
    private String status;
    private Long applicantUserId;
    private Long approverUserId;
    private String resultRemark;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String approverName;
}
