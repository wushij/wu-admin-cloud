package com.wu.cloud.message.vo;

import lombok.Data;

@Data
public class ChatPeerUnreadVO {
    private Long senderId;
    private Long unreadCount;
}
