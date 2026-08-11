package com.wu.cloud.auth.support;

/**
 * User-Agent 解析工具：从请求头中提取浏览器与操作系统信息。
 *
 * <p>仅做轻量级关键字匹配，避免为登录日志引入额外的解析库依赖。</p>
 */
public final class UserAgentUtils {

    /** 未知标识 */
    public static final String UNKNOWN = "Unknown";

    /** 数据库字段长度限制，超出部分截断，防止插入失败 */
    private static final int MAX_LENGTH = 50;

    private UserAgentUtils() {
    }

    /**
     * 解析浏览器名称
     *
     * @param userAgent 原始 User-Agent
     * @return 浏览器名称，解析不出时返回 Unknown
     */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase();

        // 注意顺序：Edge/Opera/内嵌浏览器的 UA 中同样包含 Chrome/Safari 关键字
        if (ua.contains("micromessenger")) {
            return truncate("WeChat");
        }
        if (ua.contains("miniprogram")) {
            return truncate("MiniProgram");
        }
        if (ua.contains("edg/") || ua.contains("edge")) {
            return truncate("Edge");
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return truncate("Opera");
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
        if (ua.contains("okhttp")) {
            return truncate("OkHttp");
        }
        if (ua.contains("postman")) {
            return truncate("Postman");
        }
        if (ua.contains("curl")) {
            return truncate("curl");
        }
        return UNKNOWN;
    }

    /**
     * 解析操作系统名称
     *
     * @param userAgent 原始 User-Agent
     * @return 操作系统名称，解析不出时返回 Unknown
     */
    public static String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase();

        // 移动端优先判断，避免被 Linux 等通用关键字提前命中
        if (ua.contains("iphone")) {
            return truncate("iOS");
        }
        if (ua.contains("ipad")) {
            return truncate("iPadOS");
        }
        if (ua.contains("harmony")) {
            return truncate("HarmonyOS");
        }
        if (ua.contains("android")) {
            return truncate("Android");
        }
        if (ua.contains("windows nt 10") || ua.contains("windows nt 11")) {
            return truncate("Windows 10/11");
        }
        if (ua.contains("windows")) {
            return truncate("Windows");
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return truncate("macOS");
        }
        if (ua.contains("ubuntu")) {
            return truncate("Ubuntu");
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
