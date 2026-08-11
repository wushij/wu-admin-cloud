package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单评论
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket_comment")
public class TicketCommentDO extends BaseEntity {

    private Long ticketId;
    private Long userId;
    private String content;

    @TableField(exist = false)
    private String username;
}
