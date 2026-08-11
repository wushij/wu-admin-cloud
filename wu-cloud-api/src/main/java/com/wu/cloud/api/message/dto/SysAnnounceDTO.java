package com.wu.cloud.api.message.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息公告 DTO (跨服务 RPC 传输)
 */
@Data
public class SysAnnounceDTO implements Serializable {

    private Long id;
    private String title;
    private String content;
    private Integer priority;
    private Integer targetType;
    private Integer status;
    private LocalDateTime createTime;
}
