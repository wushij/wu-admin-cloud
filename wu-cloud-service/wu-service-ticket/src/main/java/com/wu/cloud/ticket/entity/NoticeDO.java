package com.wu.cloud.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单/审批通知（本地化，后续可迁移至 message 服务）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class NoticeDO extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    /** 0 未读 1 已读 */
    private Integer readStatus;
}
