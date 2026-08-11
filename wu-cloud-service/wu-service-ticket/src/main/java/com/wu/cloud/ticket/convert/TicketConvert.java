package com.wu.cloud.ticket.convert;

import com.wu.cloud.api.ticket.dto.TicketDTO;
import com.wu.cloud.ticket.entity.TicketDO;

/**
 * 工单对象转换工具类
 */
public class TicketConvert {

    public static TicketDTO convertToDTO(TicketDO ticketDO) {
        if (ticketDO == null) {
            return null;
        }
        TicketDTO dto = new TicketDTO();
        dto.setId(ticketDO.getId());
        dto.setTitle(ticketDO.getTitle());
        dto.setDescription(ticketDO.getDescription());
        dto.setPriority(ticketDO.getPriority());
        dto.setStatus(ticketDO.getStatus());
        dto.setCreatorUserId(ticketDO.getCreatorUserId());
        dto.setAssigneeUserId(ticketDO.getAssigneeUserId());
        dto.setCreateTime(ticketDO.getCreateTime());
        return dto;
    }
}
