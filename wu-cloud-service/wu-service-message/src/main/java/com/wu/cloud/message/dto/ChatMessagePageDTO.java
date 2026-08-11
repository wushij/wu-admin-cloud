package com.wu.cloud.message.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessagePageDTO extends PageQuery {
    private Long senderId;
    private Long receiverId;
}
