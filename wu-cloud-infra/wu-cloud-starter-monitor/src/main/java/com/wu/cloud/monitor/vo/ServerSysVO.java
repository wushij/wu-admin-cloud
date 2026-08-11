package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作系统与主机信息（字段与 wu-admin-web server.ts ServerSys 对齐）
 */
@Data
public class ServerSysVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String hostName;
    private String hostAddress;
    private String osName;
    private String osVersion;
    private String userDir;
    private String javaVersion;
}
