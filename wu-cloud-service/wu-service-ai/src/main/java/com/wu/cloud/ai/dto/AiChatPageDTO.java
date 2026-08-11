package com.wu.cloud.ai.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatPageDTO extends PageQuery {
    private String question;
    private String conversationId;
}
