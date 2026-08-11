package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Redis 缓存统计信息（字段与 wu-admin-web cache.ts CacheStats 对齐）
 */
@Data
public class CacheStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long usedMemory;
    private Long maxMemory;
    private Boolean maxMemoryConfigured;
    private Long systemMemory;
    private String usedMemoryHuman;
    private Long ops;
    private Double hitRate;
    private Double cumulativeHitRate;
    private Long keyspaceHits;
    private Long keyspaceMisses;
    private Long connectedClients;
}
