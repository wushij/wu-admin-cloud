package com.wu.cloud.infra.service;

import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.SysJobLogDO;

import java.util.Map;

/**
 * 定时任务执行日志服务接口
 */
public interface SysJobLogService {

    /**
     * 记录一次任务执行日志
     */
    void record(SysJobLogDO log);

    /**
     * 调度统计（总执行数、成功/失败、成功率、近7天趋势）
     */
    Map<String, Object> statistics();

    /**
     * 分页查询执行日志
     */
    PageResult<SysJobLogDO> page(Integer pageNo, Integer pageSize,
                                 String jobName, String jobGroup, Integer status);

    /**
     * 清空全部执行日志至回收站
     */
    void cleanAll();

    /**
     * 按任务/组清空执行日志至回收站
     */
    void cleanScope(String jobName, String jobGroup);

    /**
     * 回收站分页
     */
    PageResult<SysJobLogDO> recyclePage(Integer pageNo, Integer pageSize,
                                        String jobName, String jobGroup);

    /**
     * 恢复回收站日志
     */
    void restore(Long id);

    /**
     * 彻底删除回收站日志
     */
    void deletePermanent(Long id);
}
