package com.wu.cloud.gateway.filter;

import com.wu.cloud.gateway.config.AntiReplayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AntiReplayFilter 单元测试")
class AntiReplayFilterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private AntiReplayProperties properties;
    private AntiReplayFilter filter;

    @BeforeEach
    void setUp() {
        properties = new AntiReplayProperties();
        properties.setEnabled(true);
        properties.setTimestampWindowSeconds(300L);
        properties.setExcludePaths(List.of("/api/pay/notify", "/api/file/", "/api/ws"));
        filter = new AntiReplayFilter(properties, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("未启用时直接放行")
    void disabled_passthrough() {
        properties.setEnabled(false);
        MockServerWebExchange exchange = exchange("/api/system/user/list", null, null);
        GatewayFilterChain chain = allow(exchange);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("OPTIONS 预检放行")
    void options_passthrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/system/user/list").build());
        GatewayFilterChain chain = allow(exchange);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("白名单路径放行")
    void excludedPath_passthrough() {
        MockServerWebExchange exchange = exchange("/api/pay/notify/wechat", null, null);
        GatewayFilterChain chain = allow(exchange);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("缺失时间戳返回 403")
    void missingTimestamp_rejects() {
        MockServerWebExchange exchange = exchange("/api/system/user/list", null, "nonce-1");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("非法时间戳格式返回 403")
    void badTimestamp_rejects() {
        MockServerWebExchange exchange = exchange("/api/system/user/list", "abc", "nonce-1");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("过期时间戳返回 403")
    void expiredTimestamp_rejects() {
        long old = System.currentTimeMillis() - 301_000L;
        MockServerWebExchange exchange = exchange("/api/system/user/list", String.valueOf(old), "nonce-1");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("缺失 Nonce 返回 403")
    void missingNonce_rejects() {
        MockServerWebExchange exchange = exchange("/api/system/user/list",
                String.valueOf(System.currentTimeMillis()), null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Nonce 重复返回 403")
    void duplicateNonce_rejects() {
        when(valueOps.setIfAbsent(eq("gateway:nonce:dup"), eq("1"), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.FALSE));

        MockServerWebExchange exchange = exchange("/api/system/user/list",
                String.valueOf(System.currentTimeMillis()), "dup");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Nonce 首次通过并放行")
    void freshNonce_allows() {
        when(valueOps.setIfAbsent(eq("gateway:nonce:fresh"), eq("1"), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        MockServerWebExchange exchange = exchange("/api/system/user/list",
                String.valueOf(System.currentTimeMillis()), "fresh");
        GatewayFilterChain chain = allow(exchange);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("Redis 异常时 fail-open 放行")
    void redisError_failOpen() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("redis down")));

        MockServerWebExchange exchange = exchange("/api/system/user/list",
                String.valueOf(System.currentTimeMillis()), "n1");
        GatewayFilterChain chain = allow(exchange);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("order 为 -50")
    void order_isMinus50() {
        assertEquals(-50, filter.getOrder());
    }

    private static MockServerWebExchange exchange(String path, String ts, String nonce) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (ts != null) {
            builder.header("X-Timestamp", ts);
        }
        if (nonce != null) {
            builder.header("X-Nonce", nonce);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private static GatewayFilterChain allow(MockServerWebExchange exchange) {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        return chain;
    }
}
