package com.wu.cloud.common.utils.web;

import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 可信代理感知的客户端 IP 解析核心。
 *
 * <p><b>安全要点</b>：只有在请求确实来自可信代理（默认回环与内网网段）时，才读取
 * {@code X-Real-IP} / {@code X-Forwarded-For} / {@code CF-Connecting-IP}，
 * 否则一律使用 TCP 连接的对端地址。否则攻击者直连服务端口并伪造代理头，
 * 就能伪装成任意 IP，绕过基于 IP 的封禁、登录锁定与限流。</p>
 *
 * <p>本类不依赖 Servlet API，因此 Servlet 服务（{@code ClientIpUtils}）与
 * 网关的 WebFlux 过滤器可以共用同一套判定口径，避免两边算出不同的 IP。</p>
 *
 * <p>可信代理可通过 JVM 参数覆盖：
 * {@code -Dapp.security.trusted-proxies=127.0.0.1/32,10.0.0.0/8}，
 * 或环境变量 {@code APP_SECURITY_TRUSTED_PROXIES}。</p>
 */
public final class TrustedProxyIpResolver {

    private static final String TRUSTED_PROXIES_PROPERTY = "app.security.trusted-proxies";
    private static final String TRUSTED_PROXIES_ENV = "APP_SECURITY_TRUSTED_PROXIES";

    private static final List<Cidr> TRUSTED_PROXIES = loadTrustedProxies();

    private TrustedProxyIpResolver() {
    }

    /**
     * 解析真实客户端 IP。
     *
     * @param remoteAddr         TCP 连接对端地址（未经任何头改写）
     * @param xRealIp            X-Real-IP 头
     * @param xForwardedFor      X-Forwarded-For 头
     * @param cfConnectingIp     CF-Connecting-IP 头
     * @return 真实客户端 IP，无法确定时返回 {@code "unknown"}
     */
    public static String resolve(String remoteAddr, String xRealIp, String xForwardedFor, String cfConnectingIp) {
        String remote = normalize(remoteAddr);
        if (!isTrustedProxy(remote)) {
            // 不来自可信代理：一律用 TCP 对端地址，忽略一切可伪造的头
            return StringUtils.hasText(remote) ? remote : "unknown";
        }

        // Nginx 应使用 proxy_set_header X-Real-IP $remote_addr 覆盖该头
        String realIp = normalize(firstValue(xRealIp));
        if (isValidIp(realIp)) {
            return realIp;
        }

        // 取 X-Forwarded-For 最右侧非空段：这是直连代理看到的客户端地址
        if (StringUtils.hasText(xForwardedFor)) {
            String[] parts = xForwardedFor.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = normalize(parts[i].trim());
                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }

        // Cloudflare 回源场景：Nginx 的 real_ip 模块会把 $remote_addr 还原成真实 IP，
        // 因此 X-Real-IP 已经足够；这里保留 CF 头仅作兜底，且只在可信代理来源下读取。
        String cfIp = normalize(firstValue(cfConnectingIp));
        if (isValidIp(cfIp)) {
            return cfIp;
        }
        return StringUtils.hasText(remote) ? remote : "unknown";
    }

    /**
     * 判断对端地址是否属于可信代理。
     */
    public static boolean isTrustedProxy(String ip) {
        byte[] address = toBytes(ip);
        if (address == null) {
            return false;
        }
        for (Cidr cidr : TRUSTED_PROXIES) {
            if (cidr.matches(address)) {
                return true;
            }
        }
        return false;
    }

    private static String firstValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int comma = value.indexOf(',');
        return (comma > 0 ? value.substring(0, comma) : value).trim();
    }

    private static String normalize(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "";
        }
        String trimmed = ip.trim();
        if (trimmed.startsWith("::ffff:") && trimmed.indexOf('.') > 0) {
            trimmed = trimmed.substring(7);
        }
        if (isValidIp(trimmed) && isLoopback(trimmed)) {
            return "127.0.0.1";
        }
        return trimmed;
    }

    private static boolean isLoopback(String ip) {
        try {
            return InetAddress.getByName(ip).isLoopbackAddress();
        } catch (Exception e) {
            return "127.0.0.1".equals(ip) || "::1".equals(ip);
        }
    }

    /**
     * 校验是否为合法 IP 字面量。刻意拒绝域名，避免把域名交给 getByName 触发 DNS 解析。
     */
    static boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        if (ip.indexOf(':') >= 0) {
            // 只接受 IPv6 字面量
            if (!ip.matches("[0-9a-fA-F:.]+")) {
                return false;
            }
        } else {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String part : parts) {
                if (!part.matches("\\d{1,3}")) {
                    return false;
                }
                int value = Integer.parseInt(part);
                if (value > 255) {
                    return false;
                }
            }
        }
        try {
            return InetAddress.getByName(ip) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] toBytes(String ip) {
        if (!isValidIp(ip)) {
            return null;
        }
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Cidr> loadTrustedProxies() {
        String configured = System.getProperty(TRUSTED_PROXIES_PROPERTY);
        if (!StringUtils.hasText(configured)) {
            configured = System.getenv(TRUSTED_PROXIES_ENV);
        }
        List<Cidr> result = new ArrayList<>();
        if (StringUtils.hasText(configured)) {
            for (String item : configured.split(",")) {
                Cidr cidr = Cidr.parse(item.trim());
                if (cidr != null) {
                    result.add(cidr);
                }
            }
        }
        if (!result.isEmpty()) {
            return List.copyOf(result);
        }
        List<Cidr> defaults = new ArrayList<>();
        for (String item : List.of("127.0.0.0/8", "::1/128", "10.0.0.0/8",
                "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16")) {
            Cidr cidr = Cidr.parse(item);
            if (cidr != null) {
                defaults.add(cidr);
            }
        }
        return List.copyOf(defaults);
    }

    /**
     * CIDR 网段匹配。
     */
    private record Cidr(byte[] network, int prefix) {

        static Cidr parse(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String[] parts = value.split("/", 2);
            byte[] address = toBytes(parts[0].trim());
            if (address == null) {
                return null;
            }
            int prefix = address.length * 8;
            if (parts.length == 2) {
                try {
                    prefix = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (prefix < 0 || prefix > address.length * 8) {
                return null;
            }
            return new Cidr(address, prefix);
        }

        boolean matches(byte[] candidate) {
            if (candidate == null || candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
