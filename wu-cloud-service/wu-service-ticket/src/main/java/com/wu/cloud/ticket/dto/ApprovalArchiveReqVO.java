package com.wu.cloud.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalArchiveReqVO {
    @NotNull(message = "审批单ID不能为空")
    private Long id;
    private String remark;
}
