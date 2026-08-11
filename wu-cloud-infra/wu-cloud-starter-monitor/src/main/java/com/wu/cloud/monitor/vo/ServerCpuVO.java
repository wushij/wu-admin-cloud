package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务器 CPU 监控信息（字段与 wu-admin-web server.ts ServerCpu 对齐）
 */
@Data
public class ServerCpuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String arch;
    private Integer availableProcessors;
    private Double systemLoadAverage;
    private Double systemCpuPercent;
    private Double processCpuPercent;
}
