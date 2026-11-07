package com.wu.cloud.common.security.risk;

/**
 * 风险事件上报 SPI。
 * <p>
 * 各服务在识别到异常行为（越权访问、签名失败、重放、蜜罐命中……）时上报，
 * 由风控属主服务（wu-service-system）统一计分、落库并执行封禁。
 * <p>
 * 实现由 wu-cloud-api 提供（基于 OpenFeign 调用 system 服务），
 * 未引入 api 的服务可不提供实现，此时上报被安全跳过，不影响主流程。
 */
public interface RiskReporter {

    /**
     * 上报一次风险事件。
     *
     * @param eventType 事件类型：PERMISSION_DENIED / UA_BLOCK / ANTI_REPLAY / HONEYPOT / RATE_LIMIT ...
     * @param principal 主体标识：ip:{ip} 或 user:{id}（裸 IP 也可，内部会归一化）
     * @param ip        客户端真实 IP
     * @param delta     风险分增量
     * @param action    处置动作：BLOCKED / WARNING / CHALLENGE / NOTICE
     * @param uri       请求路径
     * @param method    请求方法
     * @param userAgent 客户端 UA
     * @param reason    触发原因
     */
    void report(String eventType, String principal, String ip, int delta, String action,
                String uri, String method, String userAgent, String reason);

    /**
     * 判断主体是否处于封禁中（供入口拦截使用）；实现缺失时默认放行
     */
    default boolean isBlocked(String principal) {
        return false;
    }
}
