package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ticket")
public class TicketDO extends BaseEntity {

    private String ticketNo;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Long creatorUserId;
    private Long assigneeUserId;
    private LocalDateTime deadline;
    private LocalDateTime closedTime;

    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private String assigneeName;
}
