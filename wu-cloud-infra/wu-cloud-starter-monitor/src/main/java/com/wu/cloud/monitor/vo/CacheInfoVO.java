package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 缓存监控信息（字段与 wu-admin-web cache.ts CacheInfo 对齐）
 */
@Data
public class CacheInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Redis 是否可达 */
    private boolean available;

    /** 不可达时的原因说明 */
    private String message;

    /** INFO 命令返回的原始指标 */
    private Map<String, String> info = new LinkedHashMap<>();

    private String redisVersion;
    private String redisMode;
    private String os;
    private Integer tcpPort;
    private Long uptimeInDays;
    private Long connectedClients;
    private Long dbSize;
    private String usedMemoryHuman;
    private String usedMemoryPeakHuman;
    private Long totalCommandsProcessed;
    private Long instantaneousOpsPerSec;
}
