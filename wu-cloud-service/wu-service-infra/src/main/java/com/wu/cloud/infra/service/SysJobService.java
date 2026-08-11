package com.wu.cloud.infra.service;

import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.SysJobDO;
import com.wu.cloud.infra.vo.JobTemplateVO;
import com.wu.cloud.infra.vo.SysJobVO;

import java.util.List;
import java.util.Map;

/**
 * 定时任务服务接口
 */
public interface SysJobService {

    /**
     * 启动时加载所有正常状态任务到调度器
     */
    void initScheduler();

    /**
     * 任务与调度总览
     */
    Map<String, Object> overview();

    /**
     * 内置任务模板
     */
    List<JobTemplateVO> templates();

    /**
     * 校验 Cron 并预览下次执行时间
     */
    Map<String, Object> checkCron(String cronExpression);

    /**
     * 分页查询任务（含 cronHint / nextFireTime）
     */
    PageResult<SysJobVO> pageVo(Integer pageNo, Integer pageSize,
                                String jobName, String jobGroup, Integer status);

    /**
     * 任务详情
     */
    SysJobDO getById(Long id);

    /**
     * 新增任务
     */
    void create(SysJobDO job);

    /**
     * 更新任务
     */
    void update(SysJobDO job);

    /**
     * 删除任务（软删至回收站）
     */
    void delete(Long id);

    /**
     * 修改任务状态（0 暂停 / 1 正常）
     */
    void changeStatus(Long id, Integer status);

    /**
     * 立即执行
     */
    void run(Long id);

    /**
     * 回收站分页
     */
    PageResult<SysJobDO> recyclePage(Integer pageNo, Integer pageSize,
                                     String jobName, String jobGroup);

    /**
     * 恢复回收站任务
     */
    void restore(Long id);

    /**
     * 彻底删除回收站任务
     */
    void deletePermanent(Long id);
}
