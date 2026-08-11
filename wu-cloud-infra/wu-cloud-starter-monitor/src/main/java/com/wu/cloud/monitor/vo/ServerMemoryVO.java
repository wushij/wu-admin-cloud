package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务器内存监控信息（字段与 wu-admin-web server.ts ServerMemory 对齐）
 */
@Data
public class ServerMemoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String heapInit;
    private String heapUsed;
    private String heapMax;
    private String heapCommitted;
    private String nonHeapUsed;
    private Long heapUsedBytes;
    private Long heapMaxBytes;
    private Double heapUsedPercent;
    private String physicalTotal;
    private String physicalFree;
    private Double physicalUsedPercent;
}
