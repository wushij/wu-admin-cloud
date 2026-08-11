package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 磁盘分区信息（字段与 wu-admin-web server.ts ServerDisk 对齐）
 */
@Data
public class ServerDiskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String path;
    private String total;
    private String free;
    private String used;
    private Double usedPercent;
}
