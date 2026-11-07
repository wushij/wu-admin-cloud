package com.wu.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * 动态配置自动装配：为所有业务服务注册 {@link DynamicConfigHelper}。
 * <p>
 * 配置唯一属主为 wu-service-system（sys_config_group 表），其余服务一律通过
 * 「Redis 共享缓存 → Feign 内网回源」链路读取，彻底消除跨库直查：
 * <ul>
 *   <li>优先读取 Redis Hash {@code sys:config:groups}（由 system 服务写缓存并负责失效）；</li>
 *   <li>缓存未命中时通过 {@link ConfigGroupLoader} SPI 链上的 Feign 加载器回源
 *       {@code wu-service-system /inner/system/config/get-group} 并回填缓存。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@EnableScheduling
public class DynamicConfigAutoConfiguration {

    /**
     * 组装 SPI 加载链（Redis 优先、Feign 兜底）构建动态配置助手
     */
    @Bean
    @ConditionalOnMissingBean(DynamicConfigHelper.class)
    public DynamicConfigHelper dynamicConfigHelper(ObjectProvider<ConfigGroupLoader> loaderProvider) {
        List<ConfigGroupLoader> chain = loaderProvider.orderedStream().toList();
        log.info("[DynamicConfig] 配置加载链初始化完成，加载器数量={}（Redis 缓存优先，Feign 回源兜底）", chain.size());
        DynamicConfigHelper helper = new DynamicConfigHelper(groupCode -> {
            for (ConfigGroupLoader loader : chain) {
                try {
                    String value = loader.load(groupCode);
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                } catch (Exception e) {
                    log.warn("[DynamicConfig] 加载器 {} 读取配置组 {} 失败: {}",
                            loader.getClass().getSimpleName(), groupCode, e.getMessage());
                }
            }
            return null;
        });
        helper.preload("site", "session", "file", "rateLimit", "login", "register", "security", "sms", "email", "ai");
        return helper;
    }

    /**
     * Redis 缓存加载器：读取 system 服务维护的配置共享缓存 Hash。
     * 仅在 classpath 存在 Redis 时注册，未启用 Redis 的服务自动跳过。
     */
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisCacheLoaderConfiguration {

        @Bean
        @Order(100)
        public ConfigGroupLoader redisConfigGroupLoader(ObjectProvider<StringRedisTemplate> redisProvider) {
            return new RedisConfigGroupLoader(redisProvider);
        }
    }

    /**
     * Redis Hash 读取实现：key = sys:config:groups，field = groupCode，value = configValue JSON。
     * 命中后同步回填本进程内由 DynamicConfigHelper 持有的本地缓存。
     */
    static class RedisConfigGroupLoader implements ConfigGroupLoader {

        static final String REDIS_KEY = "sys:config:groups";

        private final ObjectProvider<StringRedisTemplate> redisProvider;

        RedisConfigGroupLoader(ObjectProvider<StringRedisTemplate> redisProvider) {
            this.redisProvider = redisProvider;
        }

        @Override
        public String load(String groupCode) {
            StringRedisTemplate template = redisProvider.getIfAvailable();
            if (template == null) {
                return null;
            }
            try {
                // Spring Data Redis 新版 opsForHash() 返回 HashOperations<String, Object, Object>，
                // 这里显式取 Object 再转字符串，避免泛型收窄导致的编译不兼容。
                Object value = template.opsForHash().get(REDIS_KEY, groupCode);
                return value == null ? null : String.valueOf(value);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
