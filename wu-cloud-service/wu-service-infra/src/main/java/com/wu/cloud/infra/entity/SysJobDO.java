package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务（映射 sys_job 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJobDO extends BaseEntity {

    /** 任务名称 */
    private String jobName;

    /** 任务组名，默认 DEFAULT */
    private String jobGroup;

    /** 调用目标字符串，如 systemJobTask.purgeExpiredLogs */
    private String invokeTarget;

    /** cron 表达式（6 段） */
    private String cronExpression;

    /** 计划执行错误策略（1 立即执行 / 2 执行一次 / 3 放弃执行） */
    private Integer misfirePolicy;

    /** 是否并发执行（0 允许 / 1 禁止） */
    private Integer concurrent;

    /** 任务状态（0 暂停 / 1 正常） */
    private Integer status;

    /** 备注 */
    private String remark;
}
