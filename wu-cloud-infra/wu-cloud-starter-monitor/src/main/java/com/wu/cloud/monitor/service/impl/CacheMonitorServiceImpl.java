package com.wu.cloud.monitor.service.impl;

import com.wu.cloud.monitor.service.CacheMonitorService;
import com.wu.cloud.monitor.vo.CacheInfoVO;
import com.wu.cloud.monitor.vo.CacheKeyItemVO;
import com.wu.cloud.monitor.vo.CacheKeysVO;
import com.wu.cloud.monitor.vo.CacheStatsVO;
import com.wu.cloud.monitor.vo.CacheValueVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.data.redis.connection.DataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 基于 Redis INFO / SCAN / TYPE / TTL 命令的缓存监控实现
 */
@Slf4j
@SuppressWarnings("deprecation")
public class CacheMonitorServiceImpl implements CacheMonitorService {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheMonitorServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CacheInfoVO getCacheInfo() {
        CacheInfoVO vo = new CacheInfoVO();
        try {
            Properties info = redisInfo();
            Long dbSize = stringRedisTemplate.execute((RedisCallback<Long>) RedisConnection::dbSize);
            if (info == null) {
                vo.setAvailable(false);
                vo.setMessage("Redis INFO 命令无返回");
                return vo;
            }
            vo.setAvailable(true);
            vo.setDbSize(dbSize);
            info.stringPropertyNames().forEach(key -> vo.getInfo().put(key, info.getProperty(key)));
            vo.setRedisVersion(info.getProperty("redis_version"));
            vo.setRedisMode(info.getProperty("redis_mode"));
            vo.setOs(info.getProperty("os"));
            vo.setTcpPort(parseInt(info.getProperty("tcp_port")));
            vo.setUptimeInDays(parseLong(info.getProperty("uptime_in_days")));
            vo.setConnectedClients(parseLong(info.getProperty("connected_clients")));
            vo.setUsedMemoryHuman(info.getProperty("used_memory_human"));
            vo.setUsedMemoryPeakHuman(info.getProperty("used_memory_peak_human"));
            vo.setTotalCommandsProcessed(parseLong(info.getProperty("total_commands_processed")));
            vo.setInstantaneousOpsPerSec(parseLong(info.getProperty("instantaneous_ops_per_sec")));
            return vo;
        } catch (Exception e) {
            log.warn("[缓存监控] Redis 指标采集失败", e);
            vo.setAvailable(false);
            vo.setMessage("Redis 连接失败：" + rootMessage(e));
            return vo;
        }
    }

    @Override
    public CacheStatsVO getStats() {
        CacheStatsVO vo = new CacheStatsVO();
        try {
            Properties info = redisInfo();
            if (info == null) {
                return vo;
            }
            Long usedMemory = parseLong(info.getProperty("used_memory"));
            Long maxMemory = parseLong(info.getProperty("maxmemory"));
            Long systemMemory = parseLong(info.getProperty("total_system_memory"));
            Long hits = parseLong(info.getProperty("keyspace_hits"));
            Long misses = parseLong(info.getProperty("keyspace_misses"));

            vo.setUsedMemory(usedMemory);
            vo.setMaxMemory(maxMemory);
            vo.setMaxMemoryConfigured(maxMemory != null && maxMemory > 0);
            vo.setSystemMemory(systemMemory);
            vo.setUsedMemoryHuman(info.getProperty("used_memory_human"));
            vo.setOps(parseLong(info.getProperty("instantaneous_ops_per_sec")));
            vo.setConnectedClients(parseLong(info.getProperty("connected_clients")));
            vo.setKeyspaceHits(hits);
            vo.setKeyspaceMisses(misses);
            vo.setHitRate(calcRate(hits, misses));
            vo.setCumulativeHitRate(calcRate(hits, misses));
            return vo;
        } catch (Exception e) {
            log.warn("[缓存监控] 统计采集失败", e);
            return vo;
        }
    }

    @Override
    public CacheKeysVO scanKeys(String pattern, long limit) {
        CacheKeysVO vo = new CacheKeysVO();
        long effectiveLimit = Math.max(1, Math.min(limit, 5000));
        String match = (pattern == null || pattern.isBlank()) ? "*" : pattern.trim();

        // 1) SCAN 收集 key 名
        List<String> keys = new ArrayList<>();
        boolean truncated = false;
        try {
            ScanOptions options = ScanOptions.scanOptions().match(match).count(200).build();
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                try (var cursor = connection.scan(options)) {
                    long scanned = 0;
                    while (cursor.hasNext()) {
                        byte[] rawKey = cursor.next();
                        scanned++;
                        if (keys.size() >= effectiveLimit) {
                            break;
                        }
                        if (scanned > effectiveLimit * 10) {
                            break;
                        }
                        keys.add(new String(rawKey, java.nio.charset.StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    log.warn("[缓存监控] SCAN 失败", e);
                }
                return null;
            });
            truncated = keys.size() >= effectiveLimit;
        } catch (Exception e) {
            log.warn("[缓存监控] SCAN 连接失败", e);
        }

        keys.sort(Comparator.naturalOrder());

        // 2) Pipeline 批量获取 TYPE + TTL（单体同款：一次 Redis 往返）
        List<CacheKeyItemVO> items = enrichKeyItems(keys);

        vo.setItems(items);
        vo.setCount((long) items.size());
        vo.setLimit(effectiveLimit);
        vo.setTruncated(truncated);
        return vo;
    }

    private List<CacheKeyItemVO> enrichKeyItems(List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<Object> pipelineResults = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                byte[] raw = stringRedisTemplate.getStringSerializer().serialize(Objects.requireNonNull(key));
                if (raw != null) {
                    connection.keyCommands().type(raw);
                    connection.keyCommands().ttl(raw);
                }
            }
            return null;
        });

        List<CacheKeyItemVO> items = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            CacheKeyItemVO item = new CacheKeyItemVO();
            item.setKey(keys.get(i));
            int base = i * 2;
            item.setType(resolvePipelineType(pipelineResults, base));
            item.setTtl(resolvePipelineTtl(pipelineResults, base + 1));
            items.add(item);
        }
        return items;
    }

    private String resolvePipelineType(List<Object> results, int index) {
        if (index >= results.size() || results.get(index) == null) {
            return "unknown";
        }
        Object raw = results.get(index);
        if (raw instanceof DataType dataType) {
            return dataType.code();
        }
        return String.valueOf(raw);
    }

    private long resolvePipelineTtl(List<Object> results, int index) {
        if (index >= results.size() || results.get(index) == null) {
            return -2L;
        }
        Object raw = results.get(index);
        if (raw instanceof Long ttl) {
            return ttl;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return -2L;
    }

    @Override
    public CacheValueVO getValue(String key) {
        CacheValueVO vo = new CacheValueVO();
        vo.setKey(key);
        vo.setType(typeOf(key));
        vo.setTtl(ttlOf(key));
        try {
            String type = vo.getType();
            if ("string".equals(type)) {
                vo.setValue(stringRedisTemplate.opsForValue().get(key));
            } else if ("hash".equals(type)) {
                vo.setValue(stringRedisTemplate.opsForHash().entries(key));
            } else if ("list".equals(type)) {
                Long len = stringRedisTemplate.opsForList().size(key);
                vo.setValue(len == null ? null : stringRedisTemplate.opsForList().range(key, 0, Math.min(len, 100) - 1));
            } else if ("set".equals(type)) {
                vo.setValue(stringRedisTemplate.opsForSet().members(key));
            } else if ("zset".equals(type)) {
                vo.setValue(stringRedisTemplate.opsForZSet().range(key, 0, 100));
            } else {
                vo.setValue("(不支持的类型: " + type + ")");
            }
        } catch (Exception e) {
            log.warn("[缓存监控] 读取 value 失败 key={}", key, e);
            vo.setValue("(读取失败: " + rootMessage(e) + ")");
        }
        return vo;
    }

    @Override
    public boolean deleteKey(String key) {
        try {
            Boolean deleted = stringRedisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.warn("[缓存监控] 删除 key 失败 key={}", key, e);
            return false;
        }
    }

    // ===== 私有工具方法 =====

    private Properties redisInfo() {
        return stringRedisTemplate.execute((RedisCallback<Properties>) RedisConnection::info);
    }

    private String typeOf(String key) {
        try {
            return stringRedisTemplate.execute((RedisCallback<String>) connection -> {
                org.springframework.data.redis.connection.DataType dataType =
                        connection.type(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return dataType == null ? "unknown" : dataType.code();
            });
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Long ttlOf(String key) {
        try {
            return stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                    connection.ttl(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 命中率（百分比）：hits / (hits + misses)，无读写时为 null
     */
    private Double calcRate(Long hits, Long misses) {
        if (hits == null || misses == null) {
            return null;
        }
        long total = hits + misses;
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(hits * 10000.0 / total) / 100.0;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        Long l = parseLong(value);
        return l == null ? null : l.intValue();
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return (message == null || message.isBlank()) ? cause.getClass().getSimpleName() : message;
    }
}
