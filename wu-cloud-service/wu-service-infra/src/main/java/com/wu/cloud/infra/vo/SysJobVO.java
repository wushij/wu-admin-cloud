package com.wu.cloud.infra.vo;

import com.wu.cloud.infra.entity.SysJobDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务视图对象（含下次执行时间与 Cron 说明）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysJobVO extends SysJobDO {

    /** 下次执行时间（已暂停则为空） */
    private String nextFireTime;

    /** Cron 可读说明 */
    private String cronHint;
}
