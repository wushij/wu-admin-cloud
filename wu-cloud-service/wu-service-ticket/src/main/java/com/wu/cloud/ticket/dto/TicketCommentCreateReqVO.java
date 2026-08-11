package com.wu.cloud.ticket.dto;

import lombok.Data;

@Data
public class TicketCommentCreateReqVO {
    private Long ticketId;
    private String content;
}
