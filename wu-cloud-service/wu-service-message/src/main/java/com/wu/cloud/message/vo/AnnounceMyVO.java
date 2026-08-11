package com.wu.cloud.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnnounceMyVO {
    private Long id;
    private String title;
    private String content;
    private Integer noticeType;
    private Integer status;
    private String createName;
    private String createAvatar;
    private LocalDateTime createTime;
    private Integer isRead;
    private LocalDateTime readTime;
}
