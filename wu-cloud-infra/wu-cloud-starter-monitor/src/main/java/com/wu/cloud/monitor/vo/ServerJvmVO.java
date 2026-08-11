package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * JVM 运行信息（字段与 wu-admin-web server.ts ServerJvm 对齐）
 */
@Data
public class ServerJvmVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String vendor;
    private String version;
    private String specVersion;
    private String startTime;
    private String uptime;
    private Long uptimeMillis;
}
