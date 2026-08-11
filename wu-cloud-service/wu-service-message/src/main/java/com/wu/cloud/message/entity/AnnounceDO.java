package com.wu.cloud.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_announce")
public class AnnounceDO extends BaseEntity {
    private String title;
    private String content;
    private Integer noticeType;
    private String channels;
    private Integer targetType;
    private String targetIds;
    private Integer status;
    private Long createBy;
    private String createName;
}
