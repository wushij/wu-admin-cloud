package com.wu.cloud.infra.util;

/**
 * User-Agent 轻量解析（登录日志、在线用户共用）
 */
public final class UserAgentUtils {

    public static final String UNKNOWN = "Unknown";
    private static final int MAX_LENGTH = 50;

    private UserAgentUtils() {
    }

    public static String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/") || ua.contains("edge")) {
            return truncate("Edge");
        }
        if (ua.contains("firefox")) {
            return truncate("Firefox");
        }
        if (ua.contains("chrome")) {
            return truncate("Chrome");
        }
        if (ua.contains("safari")) {
            return truncate("Safari");
        }
        if (ua.contains("msie") || ua.contains("trident")) {
            return truncate("IE");
        }
        return UNKNOWN;
    }

    public static String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone")) {
            return truncate("iOS");
        }
        if (ua.contains("android")) {
            return truncate("Android");
        }
        if (ua.contains("windows")) {
            return truncate("Windows");
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return truncate("macOS");
        }
        if (ua.contains("linux")) {
            return truncate("Linux");
        }
        return UNKNOWN;
    }

    private static String truncate(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
