package com.wu.cloud.ticket.dto;

import lombok.Data;

@Data
public class TicketTransitionReqVO {
    private Long id;
    private String status;
}
