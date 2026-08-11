package com.wu.cloud.common.xss;

import java.util.regex.Pattern;

/**
 * XSS 清洗工具：剥离脚本、事件属性与危险协议
 */
public final class XssUtils {

    private static final Pattern SCRIPT_TAG = Pattern.compile(
            "<script[^>]*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCRIPT_TAG_UNCLOSED = Pattern.compile(
            "</?script[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static final Pattern IFRAME_TAG = Pattern.compile(
            "<iframe[^>]*?>[\\s\\S]*?</iframe>", Pattern.CASE_INSENSITIVE);

    private static final Pattern ON_EVENT = Pattern.compile(
            "\\son[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern JAVASCRIPT_PROTOCOL = Pattern.compile(
            "javascript\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern VBSCRIPT_PROTOCOL = Pattern.compile(
            "vbscript\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern EXPRESSION = Pattern.compile(
            "expression\\s*\\(", Pattern.CASE_INSENSITIVE);

    private XssUtils() {
    }

    /**
     * 清洗字符串中的 XSS 攻击载荷
     *
     * @param value 原始字符串
     * @return 清洗后的字符串
     */
    public static String clean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String result = value;
        result = SCRIPT_TAG.matcher(result).replaceAll("");
        result = SCRIPT_TAG_UNCLOSED.matcher(result).replaceAll("");
        result = IFRAME_TAG.matcher(result).replaceAll("");
        result = ON_EVENT.matcher(result).replaceAll("");
        result = JAVASCRIPT_PROTOCOL.matcher(result).replaceAll("");
        result = VBSCRIPT_PROTOCOL.matcher(result).replaceAll("");
        result = EXPRESSION.matcher(result).replaceAll("(");
        return result;
    }
}
