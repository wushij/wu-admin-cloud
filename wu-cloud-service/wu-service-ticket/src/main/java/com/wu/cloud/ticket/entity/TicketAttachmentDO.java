package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单附件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket_attachment")
public class TicketAttachmentDO extends BaseEntity {

    private Long ticketId;
    private Long uploaderUserId;
    private String fileName;
    private String filePath;
    private Long fileSize;

    @TableField(exist = false)
    private String uploaderName;
}
