package com.wu.cloud.system.service.cache;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 配置共享缓存预热：system 启动完成后立即全量刷新 Redis Hash，
 * 保证其他微服务在启动阶段即可命中缓存，减少 Feign 回源压力。
 */
@Slf4j
@Component
public class SysConfigCacheWarmUpRunner implements ApplicationRunner {

    @Resource
    private SysConfigCacheService sysConfigCacheService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            sysConfigCacheService.refreshAll();
        } catch (Exception e) {
            log.warn("[SysConfigCache] 启动预热失败: {}", e.getMessage());
        }
    }
}
