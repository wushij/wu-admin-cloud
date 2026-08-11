package com.wu.cloud.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatGroupLogVO {
    private Long id;
    private Long groupId;
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    private String detail;
    private LocalDateTime createTime;
}
