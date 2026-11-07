package com.wu.cloud.auth.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.common.utils.crypto.Sm2Util;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * SM2 服务端密钥对管理器（与单体新版对齐）。
 *
 * <p>非对称加密的标准边界是「公钥下发、私钥只留在服务端」，因此不能像 SM4 会话密钥那样
 * 把私钥也下发给客户端。本类负责私钥的运行时托管：
 * <ul>
 *   <li>系统配置显式指定 {@code sm2PublicKey} / {@code sm2PrivateKey} 时优先使用配置（由
 *       {@link PasswordCipherService} 判断并短路）；</li>
 *   <li>未显式配置时，从 Redis 读取共享密钥对；不存在则用密码学安全随机源生成，并通过
 *       {@code setIfAbsent} 原子写入，保证多实例只生成一对、且共享同一对；</li>
 *   <li>Redis 不可用时退化为进程内临时密钥对，保证登录/注册链路不被基础设施故障拖垮。</li>
 * </ul>
 *
 * <p>私钥不会出现在任何接口响应或日志中；需要轮换时调用 {@link #rotate()}。
 */
@Component
public class Sm2KeyManager {

    private static final Logger log = LoggerFactory.getLogger(Sm2KeyManager.class);

    /** 运行时 SM2 密钥对在 Redis 中的存储键（与单体保持一致） */
    public static final String REDIS_KEY = "security:sm2:keypair";

    private static final String FIELD_PUBLIC_KEY = "publicKey";
    private static final String FIELD_PRIVATE_KEY = "privateKey";

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    /** 进程内缓存，避免每次请求都读 Redis */
    private volatile Sm2Util.Sm2KeyPair cachedKeyPair;

    /** Redis 异常降级后的重试时间点；0 表示当前缓存来自共享存储，可长期复用 */
    private volatile long fallbackRetryAt = 0L;

    public Sm2Util.Sm2KeyPair getKeyPair() {
        Sm2Util.Sm2KeyPair local = cachedKeyPair;
        long retryAt = fallbackRetryAt;
        if (local != null && (retryAt == 0L || System.currentTimeMillis() < retryAt)) {
            return local;
        }
        synchronized (this) {
            local = cachedKeyPair;
            retryAt = fallbackRetryAt;
            if (local != null && (retryAt == 0L || System.currentTimeMillis() < retryAt)) {
                return local;
            }
            // retryAt 已到期：必须重新尝试共享存储，避免 Redis 恢复后仍永久使用进程内临时密钥
            cachedKeyPair = loadOrCreate();
            return cachedKeyPair;
        }
    }

    public String getPublicKey() {
        return getKeyPair().publicKeyHex();
    }

    public String getPrivateKey() {
        return getKeyPair().privateKeyHex();
    }

    /**
     * 轮换密钥对：删除共享存储中的密钥并清空本地缓存，下次访问时重新生成。
     * <p>由于 SM2 仅用于登录/注册瞬间的口令传输，轮换不会影响存量业务数据。</p>
     */
    public void rotate() {
        synchronized (this) {
            try {
                stringRedisTemplate.delete(REDIS_KEY);
            } catch (Exception e) {
                log.warn("轮换 SM2 密钥对时清理 Redis 失败: {}", e.getMessage());
            }
            cachedKeyPair = null;
            fallbackRetryAt = 0L;
        }
    }

    private Sm2Util.Sm2KeyPair loadOrCreate() {
        try {
            Sm2Util.Sm2KeyPair stored = parse(stringRedisTemplate.opsForValue().get(REDIS_KEY));
            if (stored != null) {
                fallbackRetryAt = 0L;
                return stored;
            }

            Sm2Util.Sm2KeyPair generated = Sm2Util.generateKeyPair();
            JSONObject value = new JSONObject();
            value.set(FIELD_PUBLIC_KEY, generated.publicKeyHex());
            value.set(FIELD_PRIVATE_KEY, generated.privateKeyHex());
            Boolean created = stringRedisTemplate.opsForValue()
                    .setIfAbsent(REDIS_KEY, value.toString());
            if (Boolean.TRUE.equals(created)) {
                log.info("已生成运行时 SM2 密钥对并写入共享存储");
                fallbackRetryAt = 0L;
                return generated;
            }

            // 并发场景下其他实例已抢先写入，读取共享的那一对
            Sm2Util.Sm2KeyPair concurrent = parse(stringRedisTemplate.opsForValue().get(REDIS_KEY));
            if (concurrent != null) {
                fallbackRetryAt = 0L;
                return concurrent;
            }
            // setIfAbsent 失败但读不到共享值属于极端异常：保留临时密钥，30 秒后重试
            fallbackRetryAt = System.currentTimeMillis() + 30_000L;
            return generated;
        } catch (Exception e) {
            log.warn("读取/生成运行时 SM2 密钥对失败，30 秒内退化为进程内临时密钥对: {}", e.getMessage());
            fallbackRetryAt = System.currentTimeMillis() + 30_000L;
            return Sm2Util.generateKeyPair();
        }
    }

    private Sm2Util.Sm2KeyPair parse(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(json);
            String publicKey = obj.getStr(FIELD_PUBLIC_KEY);
            String privateKey = obj.getStr(FIELD_PRIVATE_KEY);
            if (StrUtil.isNotBlank(publicKey) && StrUtil.isNotBlank(privateKey)) {
                return new Sm2Util.Sm2KeyPair(publicKey, privateKey);
            }
        } catch (Exception e) {
            log.warn("解析共享存储中的 SM2 密钥对失败: {}", e.getMessage());
        }
        return null;
    }
}
