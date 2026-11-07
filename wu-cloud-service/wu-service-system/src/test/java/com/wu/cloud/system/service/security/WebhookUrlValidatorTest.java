package com.wu.cloud.system.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 告警 webhook SSRF 防护单元测试。
 *
 * <p>webhook 由运营在配置页填写，属于「半可信输入」。若不校验，
 * 攻击者拿到配置修改权限后即可让服务端探测内网与云元数据地址。</p>
 */
@DisplayName("WebhookUrlValidator 单元测试")
class WebhookUrlValidatorTest {

    @Test
    @DisplayName("空地址被拒绝")
    void blankRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(""));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate(null));
    }

    @Test
    @DisplayName("非 http/https 协议被拒绝")
    void nonHttpSchemeRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("gopher://example.com"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("ftp://example.com"));
    }

    @Test
    @DisplayName("携带 userinfo 的地址被拒绝（常见绕过手法）")
    void userInfoRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://user:pass@example.com/hook"));
    }

    @Test
    @DisplayName("回环地址被拒绝")
    void loopbackRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://127.0.0.1:8080/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://localhost:8080/hook"));
    }

    @Test
    @DisplayName("内网地址被拒绝")
    void privateNetworkRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://192.168.1.1/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://10.0.0.5/hook"));
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://172.16.0.1/hook"));
    }

    @Test
    @DisplayName("云元数据地址被拒绝")
    void cloudMetadataRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    @DisplayName("运营商级 NAT 网段被拒绝")
    void cgnatRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://100.64.0.1/hook"));
    }

    @Test
    @DisplayName("IPv6 唯一本地地址被拒绝")
    void ipv6UniqueLocalRejected() {
        assertThrows(IllegalArgumentException.class, () -> WebhookUrlValidator.validate("http://[fd00::1]/hook"));
    }

    @Test
    @DisplayName("无法解析的主机名被拒绝")
    void unresolvableHostRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookUrlValidator.validate("http://this-host-does-not-exist-wu-admin.invalid/hook"));
    }

    @Test
    @DisplayName("公网地址通过校验")
    void publicHostAccepted() {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("https://8.8.8.8/hook"));
    }

    @Test
    @DisplayName("脱敏输出不含路径与查询参数（机器人 token 不可泄漏到日志）")
    void maskHidesPathAndQuery() {
        String masked = WebhookUrlValidator.mask("https://oapi.example.com/robot/send?token=SECRET");
        assertTrue(masked.startsWith("https://oapi.example.com"));
        assertTrue(!masked.contains("SECRET"));
        assertTrue(!masked.contains("robot"));
    }

    @Test
    @DisplayName("非法 URL 脱敏返回兜底值")
    void maskInvalidUrl() {
        assertEquals("***", WebhookUrlValidator.mask("not a url"));
    }
}
