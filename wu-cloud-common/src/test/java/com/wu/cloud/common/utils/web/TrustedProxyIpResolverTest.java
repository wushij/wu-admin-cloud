package com.wu.cloud.common.utils.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可信代理 IP 解析单元测试。
 *
 * <p>这是风控、限流、登录锁定的共同基础：如果攻击者能伪造客户端 IP，
 * 那么基于 IP 的所有封禁与限流都可以被一行请求头绕过。</p>
 */
@DisplayName("TrustedProxyIpResolver 单元测试")
class TrustedProxyIpResolverTest {

    @Test
    @DisplayName("来自可信代理时采信 X-Real-IP")
    void trustedProxy_usesRealIp() {
        assertEquals("1.2.3.4",
                TrustedProxyIpResolver.resolve("127.0.0.1", "1.2.3.4", null, null));
        assertEquals("1.2.3.4",
                TrustedProxyIpResolver.resolve("192.168.1.10", "1.2.3.4", null, null));
    }

    @Test
    @DisplayName("来自可信代理时采信 XFF 最右侧有效段")
    void trustedProxy_usesRightmostForwardedFor() {
        assertEquals("2.2.2.2",
                TrustedProxyIpResolver.resolve("127.0.0.1", null, "1.1.1.1, 2.2.2.2", null));
    }

    @Test
    @DisplayName("来自可信代理且有 CF 头时作为兜底")
    void trustedProxy_usesCloudflareFallback() {
        assertEquals("3.3.3.3",
                TrustedProxyIpResolver.resolve("127.0.0.1", null, null, "3.3.3.3"));
    }

    @Test
    @DisplayName("直连（对端为公网地址）时忽略所有可伪造头，使用 TCP 对端地址")
    void untrustedPeer_ignoresSpoofableHeaders() {
        assertEquals("8.8.8.8",
                TrustedProxyIpResolver.resolve("8.8.8.8", "1.2.3.4", "9.9.9.9", "7.7.7.7"));
    }

    @Test
    @DisplayName("直连时伪造 XFF 无法改变判定结果（防绕过核心用例）")
    void spoofedForwardedForCannotBypassBlock() {
        // 攻击者直连并伪造 XFF，解析结果必须仍是其真实来源 IP
        String resolved = TrustedProxyIpResolver.resolve("203.0.113.7", null, "1.2.3.4", null);
        assertEquals("203.0.113.7", resolved);
    }

    @Test
    @DisplayName("可信代理未提供任何头时回落到对端地址")
    void trustedProxy_fallsBackToRemote() {
        assertEquals("127.0.0.1",
                TrustedProxyIpResolver.resolve("127.0.0.1", null, null, null));
    }

    @Test
    @DisplayName("X-Real-IP 非法时继续看 XFF")
    void invalidRealIpFallsThrough() {
        assertEquals("2.2.2.2",
                TrustedProxyIpResolver.resolve("127.0.0.1", "not-an-ip", "2.2.2.2", null));
    }

    @Test
    @DisplayName("对端地址缺失时返回 unknown")
    void missingRemoteAddress() {
        assertEquals("unknown", TrustedProxyIpResolver.resolve(null, "1.2.3.4", null, null));
    }

    @Test
    @DisplayName("IPv4-mapped IPv6 对端地址被归一化为纯 IPv4，并仍视为可信代理")
    void ipv4MappedIpv6Normalized() {
        // ::ffff:127.0.0.1 归一化为 127.0.0.1 → 属于可信代理 → 采信 X-Real-IP
        assertEquals("1.2.3.4",
                TrustedProxyIpResolver.resolve("::ffff:127.0.0.1", "1.2.3.4", null, null));
        assertTrue(TrustedProxyIpResolver.isTrustedProxy("::ffff:127.0.0.1"));
    }

    @Test
    @DisplayName("内网与回环地址属于可信代理，公网地址不属于")
    void trustedProxyDetection() {
        assertTrue(TrustedProxyIpResolver.isTrustedProxy("127.0.0.1"));
        assertTrue(TrustedProxyIpResolver.isTrustedProxy("10.1.2.3"));
        assertTrue(TrustedProxyIpResolver.isTrustedProxy("172.16.0.1"));
        assertTrue(TrustedProxyIpResolver.isTrustedProxy("192.168.0.1"));
        assertFalse(TrustedProxyIpResolver.isTrustedProxy("8.8.8.8"));
        assertFalse(TrustedProxyIpResolver.isTrustedProxy("203.0.113.7"));
        assertFalse(TrustedProxyIpResolver.isTrustedProxy("not-an-ip"));
    }
}
