package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务器监控信息汇总（字段与 wu-admin-web server.ts ServerInfo 对齐）
 */
@Data
public class ServerInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ServerCpuVO cpu = new ServerCpuVO();
    private ServerMemoryVO memory = new ServerMemoryVO();
    private ServerJvmVO jvm = new ServerJvmVO();
    private ServerSysVO sys = new ServerSysVO();
    private List<ServerDiskVO> disks = new ArrayList<>();
}
