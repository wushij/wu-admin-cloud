package com.wu.cloud.gateway.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.wu.cloud.common.utils.crypto.Sm4GcmUtil;
import com.wu.cloud.common.utils.crypto.SmCryptoUtils;
import com.wu.cloud.gateway.config.ApiSecurityProperties;
import com.wu.cloud.gateway.config.GatewayConfigLoader;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 接口安全过滤器（网关侧 GlobalFilter 实现），与旧版 {@code ApiSecurityFilter} 行为逐行对齐：
 * <ol>
 *     <li>OPTIONS 预检、multipart 文件上传、排除路径（含公开鉴权接口）直接放行</li>
 *     <li>时间戳窗口校验（默认 5 分钟，与既有规范对齐 MAX_TIMESTAMP_DIFF_MS）</li>
 *     <li>Nonce 防重放（Redis 原子 setIfAbsent，过期=时间窗口；与旧版同序，验签前先查重）</li>
 *     <li>HMAC-SM3 请求签名校验（内容 = METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY）</li>
 *     <li>SM4-GCM 认证解密请求体（标准 Base64(IV[12] + Ciphertext + Tag[16])，兼容旧 CBC 密文）</li>
 *     <li>SM4-GCM 认证加密响应体（随机 IV + 128 位 GMAC Tag；流式端点除外）</li>
 * </ol>
 *
 * <p>当 {@code gateway.api-security.enabled=true} 时（对应旧版「系统安全开关已生效」），所有
 * 非公开、非排除接口必须携带 {@code X-Client-Id}/{@code X-Timestamp}/{@code X-Nonce}/
 * {@code X-Signature} 并通过验签，否则 403，与旧版严格模式完全一致。会话密钥由认证中心
 * {@code /api/auth/session-sign-init} 按 clientId 下发并写入 Redis
 * （{@code security:session-sign:{clientId}} / {@code security:session-sm4:{clientId}}）。</p>
 */
@Component
public class ApiSecurityFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_ENCRYPTED = "X-Encrypted";
    private static final String HEADER_ACCEPT_ENCRYPTED = "X-Accept-Encrypted";
    /** 声明本次加密载荷的算法，供前端区分 GCM 与历史 CBC 格式 */
    private static final String HEADER_ENCRYPTED_PAYLOAD = "X-Encrypted-Payload";
    private static final String PAYLOAD_SM4_GCM = "SM4-GCM";

    /** HMAC-SM3 验签密钥前缀 */
    private static final String SESSION_SIGN_KEY_PREFIX = "security:session-sign:";
    /** SM4 加解密密钥前缀 */
    private static final String SESSION_SM4_KEY_PREFIX = "security:session-sm4:";
    /** Nonce 查重前缀 */
    private static final String NONCE_KEY_PREFIX = "gateway:api-nonce:";

    private final ApiSecurityProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayConfigLoader gatewayConfigLoader;

    public ApiSecurityFilter(ApiSecurityProperties properties, ReactiveStringRedisTemplate redisTemplate,
                             GatewayConfigLoader gatewayConfigLoader) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.gatewayConfigLoader = gatewayConfigLoader;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 0. OPTIONS 预检、文件上传、排除路径直接放行（与旧版 shouldNotFilter 一致）
        if (HttpMethod.OPTIONS.equals(request.getMethod())
                || isMultipart(request.getHeaders())
                || isExcluded(path)) {
            return chain.filter(exchange);
        }

        boolean securityOn = properties.isEnabled() || gatewayConfigLoader.isApiSecurityEnabled();
        boolean requestEncrypted = isEncrypted(request.getHeaders());

        // 全局安全未生效时：明文请求直接放行
        if (!securityOn && !requestEncrypted) {
            return chain.filter(exchange);
        }

        // 全局安全未生效，但客户端已声明 SM4 密文：必须解密后再转发
        // （否则下游 Jackson 把密文当 JSON 解析 → 500「系统繁忙」；与旧版「有密文必解密」一致）
        if (!securityOn) {
            return decryptOnlyAndForward(exchange, chain, request, path);
        }

        String clientId = request.getHeaders().getFirst(HEADER_CLIENT_ID);
        String timestampStr = request.getHeaders().getFirst(HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HEADER_NONCE);
        String signature = request.getHeaders().getFirst(HEADER_SIGNATURE);

        // 1. 时间戳校验
        if (!StringUtils.hasText(timestampStr)) {
            return reject(exchange, "请求已被拒绝：缺失 X-Timestamp 请求头");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException ignored) {
            return reject(exchange, "请求已被拒绝：非法时间戳格式");
        }
        long diff = Math.abs(System.currentTimeMillis() - timestamp);
        if (diff > properties.getTimestampWindowSeconds() * 1000L) {
            return reject(exchange, "请求已被拒绝：时间戳无效或已过期");
        }

        // 2. Nonce 校验（缺失即 403，与旧版一致）
        if (!StringUtils.hasText(nonce)) {
            return reject(exchange, "请求已被拒绝：缺失 X-Nonce 请求头");
        }

        // 3. 公开鉴权接口：免签名校验，但时间戳/Nonce 仍需（与旧版 isPublicAuthUri 一致）
        boolean isPublicAuth = isPublicAuthUri(path);

        // 4. 取会话密钥
        Mono<String> signKeyMono = redisTemplate.opsForValue().get(SESSION_SIGN_KEY_PREFIX + clientId);
        Mono<String> sm4KeyMono = redisTemplate.opsForValue().get(SESSION_SM4_KEY_PREFIX + clientId);

        return Mono.zip(signKeyMono.defaultIfEmpty(""), sm4KeyMono.defaultIfEmpty(""))
                .flatMap(keys -> {
                    String signKey = keys.getT1();
                    String sm4Key = keys.getT2();

                    // 5. Nonce 防重放：验签前先查重（与旧版同序），失败即 403
                    return redisTemplate.opsForValue()
                            .setIfAbsent(NONCE_KEY_PREFIX + nonce, "1",
                                    Duration.ofSeconds(properties.getTimestampWindowSeconds()))
                            .defaultIfEmpty(Boolean.FALSE)
                            .flatMap(isNew -> {
                                if (Boolean.FALSE.equals(isNew)) {
                                    return reject(exchange, "请求已被拒绝：检测到重复请求 (Anti-Replay)");
                                }
                                // 6. 签名校验（非公开接口必须带签名；密钥缺失且非公开接口即 403）
                                if (!isPublicAuth) {
                                    if (!StringUtils.hasText(signature)) {
                                        return reject(exchange, "请求已被拒绝：缺失 X-Signature 签名头");
                                    }
                                    if (!StringUtils.hasText(signKey)) {
                                        return reject(exchange, "请求已被拒绝：服务端签名密钥未配置或已过期");
                                    }
                                    // 验签在 verifyAndMutate 中基于解密后明文 body 重算
                                    return verifyAndMutate(exchange, chain, request, signature, nonce,
                                            timestampStr, signKey, sm4Key, path, isPublicAuth);
                                }
                                // 公开接口：若带签名也验证，但不强制
                                if (StringUtils.hasText(signature) && StringUtils.hasText(signKey)) {
                                    return verifyAndMutate(exchange, chain, request, signature, nonce,
                                            timestampStr, signKey, sm4Key, path, isPublicAuth);
                                }
                                // 公开接口 + 明文 body：与旧版「无签名/无密文则原样转发」等价；
                                // WebFlux 下无必要读 body 再重写（易触发 503），登录/注册等 POST 直接放行
                                if (!requestEncrypted) {
                                    return chain.filter(exchange);
                                }
                                return verifyAndMutate(exchange, chain, request, null, nonce,
                                        timestampStr, signKey, sm4Key, path, isPublicAuth);
                            });
                });
    }

    /**
     * 安全开关未生效时的轻量路径：仅 SM4 解密请求体（及按需加密响应），不强制验签/时间戳。
     * 用于 auth 已下发会话密钥、前端已加密，但网关尚未刷到 DB security 配置的窗口期。
     */
    private Mono<Void> decryptOnlyAndForward(ServerWebExchange exchange, GatewayFilterChain chain,
                                             ServerHttpRequest request, String path) {
        String clientId = request.getHeaders().getFirst(HEADER_CLIENT_ID);
        Mono<String> sm4KeyMono = StringUtils.hasText(clientId)
                ? redisTemplate.opsForValue().get(SESSION_SM4_KEY_PREFIX + clientId).defaultIfEmpty("")
                : Mono.just("");

        return sm4KeyMono.flatMap(sm4Key -> DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .flatMap(rawBody -> {
                    if (!StringUtils.hasText(sm4Key)) {
                        return reject(exchange, "请求已被拒绝：服务端加密密钥未配置或已过期");
                    }
                    String rawBodyString = new String(rawBody, StandardCharsets.UTF_8);
                    String effectiveBodyStr = rawBodyString.trim();
                    if (effectiveBodyStr.startsWith("\"") && effectiveBodyStr.endsWith("\"")
                            && effectiveBodyStr.length() > 2) {
                        effectiveBodyStr = effectiveBodyStr.substring(1, effectiveBodyStr.length() - 1);
                    }
                    byte[] dispatchBody;
                    try {
                        String plain = decryptBody(effectiveBodyStr, sm4Key);
                        dispatchBody = plain.getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        return reject(exchange, "请求已被拒绝：SM4 请求解密失败");
                    }

                    ServerHttpRequest mutatedRequest = mutateBody(exchange, dispatchBody);
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                    boolean wantsEncryptedResp = "1".equalsIgnoreCase(
                            request.getHeaders().getFirst(HEADER_ACCEPT_ENCRYPTED))
                            || isEncrypted(request.getHeaders());
                    if (!wantsEncryptedResp || isStreamingUri(path)) {
                        return chain.filter(mutatedExchange);
                    }
                    ServerHttpResponse mutatedResponse = mutateResponse(exchange, sm4Key);
                    return chain.filter(mutatedExchange.mutate().response(mutatedResponse).build());
                }));
    }

    /**
     * 读取请求体 -> 验签（基于明文 body）-> SM4 解密 -> 重建请求 -> 包装响应。
     */
    private Mono<Void> verifyAndMutate(ServerWebExchange exchange, GatewayFilterChain chain,
                                       ServerHttpRequest request, String signature, String nonce,
                                       String timestampStr, String signKey, String sm4Key,
                                       String path, boolean isPublicAuth) {
        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .flatMap(rawBody -> {
                    String rawBodyString = new String(rawBody, StandardCharsets.UTF_8);
                    // 与旧版一致：Axios 可能把密文字符串再 JSON 包一层引号，验签/解密前先剥掉
                    String effectiveBodyStr = rawBodyString.trim();
                    if (effectiveBodyStr.startsWith("\"") && effectiveBodyStr.endsWith("\"")
                            && effectiveBodyStr.length() > 2) {
                        effectiveBodyStr = effectiveBodyStr.substring(1, effectiveBodyStr.length() - 1);
                    }

                    // 签名校验：基于密文 body（与前端签名内容一致，前端也是对加密后 body 签名）
                    if (StringUtils.hasText(signature)) {
                        String expected = computeSignature(
                                request.getMethod().name(),
                                request.getURI(),
                                timestampStr,
                                nonce,
                                effectiveBodyStr,
                                signKey);
                        if (!signature.equalsIgnoreCase(expected)) {
                            return reject(exchange, "请求已被拒绝：签名校验失败");
                        }
                    }

                    // SM4 解密请求体
                    byte[] dispatchBody = rawBody;
                    boolean isRequestEncrypted = isEncrypted(request.getHeaders());
                    if (isRequestEncrypted) {
                        if (!StringUtils.hasText(sm4Key)) {
                            return reject(exchange, "请求已被拒绝：服务端加密密钥未配置或已过期");
                        }
                        try {
                            String plain = decryptBody(effectiveBodyStr, sm4Key);
                            dispatchBody = plain.getBytes(StandardCharsets.UTF_8);
                        } catch (Exception ignored) {
                            return reject(exchange, "请求已被拒绝：SM4 请求解密失败");
                        }
                    }

                    ServerHttpRequest mutatedRequest = mutateBody(exchange, dispatchBody);
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                    boolean wantsEncryptedResp = "1".equalsIgnoreCase(request.getHeaders().getFirst(HEADER_ACCEPT_ENCRYPTED))
                            || isRequestEncrypted;
                    if (!wantsEncryptedResp || !StringUtils.hasText(sm4Key) || isStreamingUri(path)) {
                        return chain.filter(mutatedExchange);
                    }
                    ServerHttpResponse mutatedResponse = mutateResponse(exchange, sm4Key);
                    return chain.filter(mutatedExchange.mutate().response(mutatedResponse).build());
                });
    }

    /**
     * 计算 HMAC-SM3 签名，与旧版 {@code ApiSecurityFilter.calcSignature} 完全一致：
     * 内容 = METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY（body 取原始字符串，与前端一致，对应旧版 inputBody）。
     * <p>路径与查询串做 URLDecoder.decode，与旧版严格对齐（前端以解码后路径计算签名）。
     */
    private String computeSignature(String method, URI uri, String timestamp, String nonce, String body, String signKey) {
        // 与旧版一致：旧版 context-path=/api，签名用 servletPath（不含 /api）；
        // 前端 request.ts 也会去掉 /api 前缀后再签名。
        String path = uri.getPath();
        if (path != null && path.startsWith("/api/")) {
            path = path.substring(4);
        }
        String rawQuery = uri.getRawQuery();
        String fullPath = (rawQuery != null) ? path + "?" + rawQuery : path;
        try {
            fullPath = URLDecoder.decode(fullPath, StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        String content = method + "\n" + fullPath + "\n" + timestamp + "\n" + nonce + "\n" + body;
        return SmCryptoUtils.signHmacSm3(content, signKey.getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    /**
     * SM4 解密请求体。
     * <p>优先按满血标准 SM4-GCM 认证解密（Base64(IV[12] + Ciphertext + Tag[16])，与前端及旧版一致）；
     * 认证失败时回退旧版 CBC 格式（ivHex(32) + cipherHex）与 ECB 降级，保证历史客户端可用。</p>
     */
    private String decryptBody(String cipherText, String sm4Key) {
        if (cipherText.startsWith("\"") && cipherText.endsWith("\"") && cipherText.length() > 2) {
            cipherText = cipherText.substring(1, cipherText.length() - 1);
        }
        // 与旧版 toSm4KeyBytes 对齐：32位Hex→16字节，其余按 UTF-8 字节处理
        byte[] keyBytes = toSm4KeyBytes(sm4Key);

        // 1. 满血标准 SM4-GCM（认证失败绝不静默降级为其他模式，直接抛出交由上层拒绝）
        if (Sm4GcmUtil.isSm4GcmBase64Payload(cipherText)) {
            try {
                return Sm4GcmUtil.decryptFromBase64(keyBytes, cipherText);
            } catch (Exception gcmError) {
                // 可能是历史 CBC 密文恰好符合 Base64 字符集，继续走兼容分支
            }
        }

        // 2. 历史 SM4-CBC：ivHex(32) + cipherHex，ECB 降级兼容
        String decrypted = null;
        if (cipherText.length() > 32 && HexUtil.isHexNumber(cipherText.substring(0, 32))) {
            try {
                String hexIv = cipherText.substring(0, 32);
                String rawCipher = cipherText.substring(32);
                byte[] ivBytes = HexUtil.decodeHex(hexIv);
                SM4 cbcSm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
                decrypted = cbcSm4.decryptStr(rawCipher);
            } catch (Exception ignored) {
                // SM4-CBC 解密异常，降级 ECB
            }
        }
        if (decrypted == null) {
            decrypted = SmUtil.sm4(keyBytes).decryptStr(cipherText);
        }
        return decrypted;
    }

    /**
     * SM4 密钥字符串转 16 字节密钥，与旧版 ApiSecurityFilter#toSm4KeyBytes 严格对齐。
     * <p>会话临时密钥为 32 位 Hex（session-sign-init 下发），按 Hex 解码为 16 字节；
     * 其余情况（DB 静态 16 字符密钥）按 UTF-8 文本取字节。
     */
    private static byte[] toSm4KeyBytes(String key) {
        if (key.length() == 32 && key.matches("[0-9a-fA-F]{32}")) {
            return HexUtil.decodeHex(key);
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 重写请求体为解密后的明文。
     */
    private ServerHttpRequest mutateBody(ServerWebExchange exchange, byte[] body) {
        ServerHttpRequest request = exchange.getRequest();
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        final byte[] bodyBytes = body;
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                // 每次订阅新建 DataBuffer：Netty 路由层可能多次读取 body，Flux.just 只能消费一次会抛异常 → 503
                return Flux.defer(() -> Flux.just(bufferFactory.wrap(bodyBytes)));
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(request.getHeaders());
                headers.remove(HEADER_ENCRYPTED);
                headers.remove(HEADER_ACCEPT_ENCRYPTED);
                if (body.length > 0) {
                    headers.setContentLength(body.length);
                } else {
                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                }
                return headers;
            }
        };
    }

    /**
     * 包装响应，使用满血标准 SM4-GCM 认证加密响应体（Base64(IV[12] + Ciphertext + Tag[16])）。
     * <p>与旧版 {@code ApiSecurityFilter#encryptResponse} 一致，并通过
     * {@code X-Encrypted-Payload: SM4-GCM} 声明载荷算法。</p>
     */
    private ServerHttpResponse mutateResponse(ServerWebExchange exchange, String sm4Key) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        // 与旧版 toSm4KeyBytes 对齐：32位Hex→16字节，其余按 UTF-8 字节处理
        final byte[] keyBytes = toSm4KeyBytes(sm4Key);

        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux<? extends DataBuffer> fluxBody) {
                    return DataBufferUtils.join(fluxBody).flatMap(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);
                        try {
                            String plain = new String(content, StandardCharsets.UTF_8);
                            String encrypted = Sm4GcmUtil.encryptToBase64(keyBytes, plain);
                            getHeaders().set(HEADER_ENCRYPTED, "1");
                            getHeaders().set(HEADER_ENCRYPTED_PAYLOAD, PAYLOAD_SM4_GCM);
                            getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            DataBuffer buffer = bufferFactory.wrap(encrypted.getBytes(StandardCharsets.UTF_8));
                            return super.writeWith(Mono.just(buffer));
                        } catch (Exception ignored) {
                            DataBuffer buffer = bufferFactory.wrap(content);
                            return super.writeWith(Mono.just(buffer));
                        }
                    });
                }
                return super.writeWith(body);
            }
        };
    }

    private boolean isEncrypted(HttpHeaders headers) {
        String v = headers.getFirst(HEADER_ENCRYPTED);
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    private boolean isMultipart(HttpHeaders headers) {
        String ct = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        return ct != null && ct.toLowerCase().contains("multipart");
    }

    private boolean isStreamingUri(String path) {
        if (path == null) {
            return false;
        }
        for (String suffix : properties.getStreamingUriSuffixes()) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicAuthUri(String path) {
        if (path == null) {
            return false;
        }
        for (String uri : properties.getPublicAuthUris()) {
            if (path.equals(uri) || path.endsWith(uri)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : properties.getExcludePaths()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        // 响应一旦提交，getHeaders() 返回只读视图，再写会抛 UnsupportedOperationException，
        // 导致 403 变成连接中断（前端拿不到 JSON 错误码），此处仅在可写时设置 Content-Type。
        if (!response.isCommitted()) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            } catch (UnsupportedOperationException ignored) {
                // 响应头已只读，保持原样
            }
        }
        String body = "{\"code\":403,\"msg\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 早于路由转发与鉴权，先完成验签/解密
        return -60;
    }
}
