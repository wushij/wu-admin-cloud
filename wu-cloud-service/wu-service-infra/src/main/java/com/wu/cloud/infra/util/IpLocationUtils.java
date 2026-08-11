package com.wu.cloud.infra.util;

import org.springframework.util.StringUtils;

/**
 * IP 归属地（无 ip2region 时降级为内网/未知）
 */
public final class IpLocationUtils {

    private IpLocationUtils() {
    }

    public static String resolve(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "未知";
        }
        String trimmed = ip.trim();
        if (isPrivateIp(trimmed)) {
            return "内网IP";
        }
        return "未知";
    }

    private static boolean isPrivateIp(String ip) {
        if (ip.startsWith("127.") || "localhost".equalsIgnoreCase(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
