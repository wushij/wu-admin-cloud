package com.wu.cloud.common.security.risk;

/**
 * 风控 Redis Key 命名契约。
 *
 * <p>网关（请求侧）与 system 服务（管理侧）读写同一批 Key，必须共享同一份定义，
 * 否则会出现「网关写入的封禁 system 服务读不到」这类静默失效。</p>
 *
 * <p>命名空间说明：</p>
 * <ul>
 *   <li>{@code security:risk:h:*} —— 风险分窗口（Hash，分桶计数）</li>
 *   <li>{@code security:risk:block:*} —— 封禁标记（String，值为封禁原因）</li>
 *   <li>{@code security:risk:pass:*} —— 挑战通过豁免（String）</li>
 *   <li>{@code security:risk:cd:*} —— 各类去重标记（告警去重、行为计数）</li>
 * </ul>
 */
public final class SecurityRedisKeys {

    /** 风险分窗口前缀（Hash，分桶计数） */
    public static final String RISK_WINDOW_PREFIX = "security:risk:h:";

    /** 封禁标记前缀，value 为封禁原因 */
    public static final String BLOCK_PREFIX = "security:risk:block:";

    /** 挑战通过后的豁免前缀 */
    public static final String CHALLENGE_PASS_PREFIX = "security:risk:pass:";

    /** 告警去重键前缀：同一主体 + 同一事件类型 + 同一等级在去重时间内只推送一次 */
    public static final String ALERT_DEDUP_PREFIX = "security:risk:cd:alert:";

    /** 爬虫 UA 命中计数前缀 */
    public static final String UA_HIT_PREFIX = "security:risk:cd:ua:";

    /** 页面扫描行为计数前缀 */
    public static final String PAGE_SCAN_PREFIX = "security:risk:cd:page:";

    /** 批量导出行为计数前缀 */
    public static final String EXPORT_PREFIX = "security:risk:cd:export:";

    private SecurityRedisKeys() {
    }

    public static String riskWindow(String principalKey) {
        return RISK_WINDOW_PREFIX + principalKey;
    }

    public static String block(String principalKey) {
        return BLOCK_PREFIX + principalKey;
    }

    public static String challengePass(String principalKey) {
        return CHALLENGE_PASS_PREFIX + principalKey;
    }
}
