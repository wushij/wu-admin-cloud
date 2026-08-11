package com.wu.cloud.monitor.service;

import com.wu.cloud.monitor.vo.ServerInfoVO;

/**
 * 服务器实时监控服务
 */
public interface ServerMonitorService {

    /**
     * 采集当前节点的 CPU、内存、JVM、磁盘与系统信息
     *
     * @return 服务器监控快照
     */
    ServerInfoVO getServerInfo();
}
