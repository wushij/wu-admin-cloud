package com.wu.cloud.api.ticket.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单 DTO (跨服务 RPC 传输)
 */
@Data
public class TicketDTO implements Serializable {

    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Long creatorUserId;
    private Long assigneeUserId;
    private LocalDateTime createTime;
}
