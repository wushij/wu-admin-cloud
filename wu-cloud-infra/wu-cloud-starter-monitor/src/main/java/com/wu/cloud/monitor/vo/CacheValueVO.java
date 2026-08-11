package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Redis 缓存 value 详情（字段与 wu-admin-web cache.ts CacheValueDetail 对齐）
 */
@Data
public class CacheValueVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String type;
    private Long ttl;
    private Object value;
}
