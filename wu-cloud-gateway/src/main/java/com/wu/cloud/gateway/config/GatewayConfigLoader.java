package com.wu.cloud.gateway.config;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关配置加载器：通过 WebClient + LoadBalancer 调用 system 内部接口读取 security 配置组，本地缓存。
 * <p>
 * 注意：不可用裸主机名 {@code http://wu-service-system:9201}（无 LB 时会被 Clash/Hosts 解析到错误 IP），
 * 必须走 {@code lb://wu-service-system}；本地调试可配置 {@code gateway.config-source-url}。
 * </p>
 */
@Component
@EnableScheduling
public class GatewayConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfigLoader.class);

    @Value("${gateway.config-source-url:http://wu-service-system/inner/system/config/get-group}")
    private String configSourceUrl;

    private final Map<String, JSONObject> cache = new ConcurrentHashMap<>();
    private final WebClient webClient;

    public GatewayConfigLoader(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        this.webClient = WebClient.builder().filter(lbFunction).build();
    }

    @PostConstruct
    @Scheduled(fixedDelay = 60_000, initialDelay = 3_000)
    public void refresh() {
        loadGroup("security");
        loadGroup("rateLimit");
    }

    private void loadGroup(String groupCode) {
        try {
            webClient.get()
                    .uri(configSourceUrl + "?groupCode=" + groupCode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .subscribe(body -> {
                        try {
                            JSONObject resp = JSONUtil.parseObj(body);
                            Object data = resp.get("data");
                            if (data != null) {
                                JSONObject dataObj = data instanceof JSONObject
                                        ? (JSONObject) data
                                        : JSONUtil.parseObj(data.toString());
                                String configValue = dataObj.getStr("configValue");
                                if (configValue != null && !configValue.isBlank()) {
                                    cache.put(groupCode, JSONUtil.parseObj(configValue));
                                    log.debug("[GatewayConfig] 已刷新配置组 {}", groupCode);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("[GatewayConfig] 解析配置组 {} 失败: {}", groupCode, e.getMessage());
                        }
                    }, err -> log.warn("[GatewayConfig] 加载配置组 {} 失败: {}", groupCode, err.getMessage()));
        } catch (Exception e) {
            log.warn("[GatewayConfig] 加载配置组 {} 异常: {}", groupCode, e.getMessage());
        }
    }

    public JSONObject getGroup(String groupCode) {
        return cache.getOrDefault(groupCode, new JSONObject());
    }

    public boolean getBool(String groupCode, String key, boolean defaultValue) {
        return getGroup(groupCode).getBool(key, defaultValue);
    }

    /**
     * 与单体一致：DB security 组开启 SM2 签名或 SM4 加密任一，即视为接口安全生效。
     */
    public boolean isApiSecurityEnabled() {
        return getBool("security", "sm2SignEnabled", false)
                || getBool("security", "sm4EncryptEnabled", false);
    }
}
