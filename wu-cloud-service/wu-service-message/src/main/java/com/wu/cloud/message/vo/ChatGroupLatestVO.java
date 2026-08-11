package com.wu.cloud.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatGroupLatestVO {
    private Long groupId;
    private String content;
    private Integer msgType;
    private String senderName;
    private LocalDateTime sendTime;
}
