package com.wu.cloud.monitor.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 缓存 key 扫描结果（字段与 wu-admin-web cache.ts CacheKeysResult 对齐）
 */
@Data
public class CacheKeysVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CacheKeyItemVO> items = new ArrayList<>();
    private Long count;
    private Long limit;
    private Boolean truncated;
}
