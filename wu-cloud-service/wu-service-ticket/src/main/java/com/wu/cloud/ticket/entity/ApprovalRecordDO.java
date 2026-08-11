package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_record")
public class ApprovalRecordDO extends BaseEntity {

    private Long formId;
    private Long operatorUserId;
    /** SUBMIT/APPROVE/REJECT/ARCHIVE/WITHDRAW */
    private String action;
    private String remark;

    @TableField(exist = false)
    private String operatorName;
}
