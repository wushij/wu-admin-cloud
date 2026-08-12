package com.wu.cloud.gateway.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import com.wu.cloud.common.utils.crypto.SmCryptoUtils;
import com.wu.cloud.gateway.config.ApiSecurityProperties;
import com.wu.cloud.gateway.config.GatewayConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApiSecurityFilter SM4 解密单测")
class ApiSecurityFilterTest {

    private static final String CLIENT_ID = "test-client-01";
    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String SIGN_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;
    @Mock
    private GatewayConfigLoader gatewayConfigLoader;

    private ApiSecurityProperties properties;
    private ApiSecurityFilter filter;

    @BeforeEach
    void setUp() {
        properties = new ApiSecurityProperties();
        properties.setEnabled(false);
        filter = new ApiSecurityFilter(properties, redisTemplate, gatewayConfigLoader);
        when(gatewayConfigLoader.isApiSecurityEnabled()).thenReturn(false);
    }

    @Test
    @DisplayName("安全关闭且明文请求：直接放行")
    void plaintext_passthrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/message/chat/send")
                        .body("{\"content\":\"hi\"}"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("X-Encrypted=1：解密请求体后再转发")
    void decryptOnly_whenEncryptedHeader() {
        String plain = "{\"content\":\"hello-sm4\"}";
        String cipherBody = encryptCbc(plain, SM4_KEY);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("security:session-sm4:" + CLIENT_ID)).thenReturn(Mono.just(SM4_KEY));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/message/chat/send")
                        .header("X-Encrypted", "1")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Accept-Encrypted", "0")
                        .body(cipherBody));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ServerWebExchange mutated = captor.getValue();
        assertNotSame(exchange, mutated);
        String forwarded = readBody(mutated);
        assertEquals(plain, forwarded);
    }

    @Test
    @DisplayName("X-Encrypted=1 但密钥缺失：403")
    void decryptOnly_missingKey_rejects() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(Mono.just(""));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/message/chat/send")
                        .header("X-Encrypted", "1")
                        .header("X-Client-Id", CLIENT_ID)
                        .body("deadbeef"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("安全开启：缺失时间戳拒绝")
    void securityOn_missingTimestamp_rejects() {
        properties.setEnabled(true);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/system/user/list")
                        .header("X-Client-Id", CLIENT_ID)
                        .body("{}"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("安全开启：缺失 Nonce 拒绝")
    void securityOn_missingNonce_rejects() {
        properties.setEnabled(true);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/system/user/list")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .body("{}"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("安全开启：Nonce 重放拒绝")
    void securityOn_replayNonce_rejects() {
        properties.setEnabled(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(startsWith("security:session-sign:"))).thenReturn(Mono.just(SIGN_KEY));
        when(valueOps.get(startsWith("security:session-sm4:"))).thenReturn(Mono.just(SM4_KEY));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(false));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/system/user/list")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .header("X-Nonce", "nonce-1")
                        .header("X-Signature", "ABCD")
                        .body("{}"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("安全开启：签名错误拒绝")
    void securityOn_badSignature_rejects() {
        properties.setEnabled(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(startsWith("security:session-sign:"))).thenReturn(Mono.just(SIGN_KEY));
        when(valueOps.get(startsWith("security:session-sm4:"))).thenReturn(Mono.just(SM4_KEY));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/system/user/list")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .header("X-Nonce", "nonce-bad")
                        .header("X-Signature", "DEADBEEF")
                        .body("{}"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("安全开启：公开登录接口免签且明文 POST 直接放行（不读 body）")
    void securityOn_publicLogin_passthroughWithoutBodyRead() {
        properties.setEnabled(true);
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        String ts = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-login-01";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(startsWith("security:session-sign:"))).thenReturn(Mono.just(""));
        when(valueOps.get(startsWith("security:session-sm4:"))).thenReturn(Mono.just(""));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .body(body));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("安全开启：验签通过后转发明文请求体")
    void securityOn_validSignature_forwards() {
        properties.setEnabled(true);
        String body = "{\"q\":1}";
        String ts = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-ok-01";
        String signature = sign("POST", "/system/user/list", ts, nonce, body, SIGN_KEY);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(startsWith("security:session-sign:"))).thenReturn(Mono.just(SIGN_KEY));
        when(valueOps.get(startsWith("security:session-sm4:"))).thenReturn(Mono.just(SM4_KEY));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/system/user/list")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .header("X-Signature", signature)
                        .body(body));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(body, readBody(captor.getValue()));
    }

    @Test
    @DisplayName("安全开启：验签通过且 SM4 密文请求体解密后转发")
    void securityOn_validSignature_decryptsBody() {
        properties.setEnabled(true);
        String plain = "{\"content\":\"ok\"}";
        String cipher = encryptCbc(plain, SM4_KEY);
        String ts = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-sm4-01";
        String signature = sign("POST", "/message/chat/send", ts, nonce, cipher, SIGN_KEY);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(startsWith("security:session-sign:"))).thenReturn(Mono.just(SIGN_KEY));
        when(valueOps.get(startsWith("security:session-sm4:"))).thenReturn(Mono.just(SM4_KEY));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any())).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/message/chat/send")
                        .header("X-Client-Id", CLIENT_ID)
                        .header("X-Timestamp", ts)
                        .header("X-Nonce", nonce)
                        .header("X-Signature", signature)
                        .header("X-Encrypted", "1")
                        .header("X-Accept-Encrypted", "0")
                        .body(cipher));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertEquals(plain, readBody(captor.getValue()));
    }

    private static String sign(String method, String pathWithoutApi, String ts, String nonce,
                               String body, String signKey) {
        String content = method + "\n" + pathWithoutApi + "\n" + ts + "\n" + nonce + "\n" + body;
        return SmCryptoUtils.signHmacSm3(content, signKey.getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    private static String encryptCbc(String plain, String sm4KeyHex) {
        byte[] keyBytes = HexUtil.decodeHex(sm4KeyHex);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, iv);
        return HexUtil.encodeHexStr(iv) + sm4.encryptHex(plain);
    }

    private static String readBody(ServerWebExchange exchange) {
        DataBuffer buffer = DataBufferUtils.join(exchange.getRequest().getBody()).block();
        assertNotNull(buffer);
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        DataBufferUtils.release(buffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
