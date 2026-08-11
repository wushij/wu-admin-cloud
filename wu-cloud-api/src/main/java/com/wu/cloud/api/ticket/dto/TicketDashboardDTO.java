package com.wu.cloud.api.ticket.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单/审批工作台统计 DTO（跨服务 RPC 传输）
 */
@Data
public class TicketDashboardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理工单数（OPEN 状态） */
    private Long ticketOpenCount = 0L;

    /** 逾期工单数（OPEN/IN_PROGRESS 且超过截止时间） */
    private Long ticketOverdueCount = 0L;

    /** 待审批数（SUBMITTED 状态） */
    private Long approvalPendingCount = 0L;
}
