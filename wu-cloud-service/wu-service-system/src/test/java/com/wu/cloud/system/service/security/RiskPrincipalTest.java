package com.wu.cloud.system.service.security;

import com.wu.cloud.common.security.risk.RiskPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 风险主体归一化单元测试。
 *
 * <p>主体归一化是风控的口径基础：写封禁与读封禁必须落在同一个 key 上，
 * 否则会出现「已封禁但永远匹配不上」的静默失效。</p>
 */
@DisplayName("RiskPrincipal 单元测试")
class RiskPrincipalTest {

    @Test
    @DisplayName("裸 IP 归一化为 ip: 前缀主体")
    void normalize_bareIp() {
        RiskPrincipal principal = RiskPrincipal.normalize("1.2.3.4", null);
        assertEquals("ip:1.2.3.4", principal.key());
        assertEquals(RiskPrincipal.TYPE_IP, principal.type());
        assertEquals("1.2.3.4", principal.ip());
        assertNull(principal.userId());
        assertFalse(principal.isUser());
    }

    @Test
    @DisplayName("带 ip: 前缀的主体不被二次加前缀")
    void normalize_ipPrefixed() {
        RiskPrincipal principal = RiskPrincipal.normalize("ip:5.6.7.8", null);
        assertEquals("ip:5.6.7.8", principal.key());
        assertEquals("5.6.7.8", principal.ip());
    }

    @Test
    @DisplayName("带 user: 前缀的主体保留用户 ID")
    void normalize_userPrefixed() {
        RiskPrincipal principal = RiskPrincipal.normalize("user:42", "1.2.3.4");
        assertEquals("user:42", principal.key());
        assertEquals(RiskPrincipal.TYPE_USER, principal.type());
        assertEquals(42L, principal.userId());
        assertTrue(principal.isUser());
    }

    @Test
    @DisplayName("主体为空时回退到 IP")
    void normalize_fallbackToIp() {
        RiskPrincipal principal = RiskPrincipal.normalize(null, "9.9.9.9");
        assertEquals("ip:9.9.9.9", principal.key());
        assertEquals("9.9.9.9", principal.ip());
    }

    @Test
    @DisplayName("IP 为空时使用 unknown 占位，避免空串进 Redis")
    void normalize_blankIpUsesUnknown() {
        RiskPrincipal principal = RiskPrincipal.normalize(null, null);
        assertEquals("ip:unknown", principal.key());
        assertEquals("unknown", principal.ip());
    }

    @Test
    @DisplayName("user: 前缀但 ID 非数字时 userId 为空，不抛异常")
    void normalize_malformedUserId() {
        RiskPrincipal principal = RiskPrincipal.normalize("user:abc", null);
        assertEquals("user:abc", principal.key());
        assertNull(principal.userId());
    }

    @Test
    @DisplayName("空白 key 拒绝构造")
    void blankKeyRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RiskPrincipal("  ", "ip", null, null));
    }

    @Test
    @DisplayName("displayId：用户显示 ID，IP 显示地址")
    void displayId() {
        assertEquals("42", RiskPrincipal.ofUser(42L, "1.1.1.1").displayId());
        assertEquals("1.1.1.1", RiskPrincipal.ofIp("1.1.1.1").displayId());
    }
}
