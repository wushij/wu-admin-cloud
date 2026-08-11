package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Redis 缓存 key 项（字段与 wu-admin-web cache.ts CacheKeyItem 对齐）
 */
@Data
public class CacheKeyItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String type;
    private Long ttl;
}
