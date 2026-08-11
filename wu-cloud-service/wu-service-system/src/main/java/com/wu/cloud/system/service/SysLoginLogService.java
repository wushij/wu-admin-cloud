package com.wu.cloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysLoginLogDO;

/**
 * 登录日志服务
 */
public interface SysLoginLogService {

    /**
     * 记录一条登录日志
     */
    void recordLoginLog(SysLoginLogDO logDO);

    /**
     * 按账号分页查询登录记录
     */
    Page<SysLoginLogDO> pageByUsername(String username, Integer pageNo, Integer pageSize);

    /**
     * 后台管理分页查询登录日志（支持账号、IP、状态过滤）
     *
     * @param pageQuery 分页参数
     * @param username  账号，模糊匹配，可为空
     * @param ip        登录 IP，模糊匹配，可为空
     * @param status    登录状态：0 成功 1 失败，可为空
     */
    Page<SysLoginLogDO> getLoginLogPage(PageQuery pageQuery, String username, String ip, Integer status);

    /**
     * 删除单条登录日志
     */
    void deleteLoginLog(Long id);

    /**
     * 清空全部登录日志
     */
    void clearLoginLog();
}
