package com.wu.cloud.ticket.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单创建请求 DTO
 */
@Data
public class TicketCreateReqVO implements Serializable {

    private String title;
    private String description;
    private String priority;
    private Long assigneeUserId;
    private LocalDateTime deadline;
}
