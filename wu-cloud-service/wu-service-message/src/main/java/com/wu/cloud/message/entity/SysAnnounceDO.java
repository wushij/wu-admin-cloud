package com.wu.cloud.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息公告 DO 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_announce")
public class SysAnnounceDO extends BaseEntity {

    private String title;
    private String content;
    private Integer priority;
    private Integer targetType;
    private Integer status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
