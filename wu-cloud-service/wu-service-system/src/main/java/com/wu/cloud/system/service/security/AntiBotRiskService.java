package com.wu.cloud.system.service.security;

import com.wu.cloud.common.security.risk.RiskPrincipal;
import com.wu.cloud.system.entity.SecurityEventDO;
import com.wu.cloud.system.vo.RiskSubjectVO;

import java.util.List;

/**
 * 行为风控与反爬防御服务。
 *
 * <p>计分模型：风险分按主体（账号 / IP）落在 Redis 的分桶计数滑动窗口里，
 * 读取时只汇总最近一个滑动窗口内的分值，超过窗口的分值自然失效，
 * 因此不需要额外的"衰减任务"，也不会出现分数只增不减导致的永久黑名单。</p>
 *
 * <p>本服务是风控的<b>唯一属主</b>：计分、封禁、事件落库都在 system 服务完成。
 * 网关侧的风控拦截通过 Redis Key 契约（{@code SecurityRedisKeys}）读取同一份状态，
 * 不重复实现计分逻辑。</p>
 */
public interface AntiBotRiskService {

    /**
     * 兼容旧签名的计分入口。
     *
     * <p>蜜罐等既有调用方传入的是裸 IP，内部会统一归一化为 {@code ip:} 主体。</p>
     */
    int addRisk(String principal, String ip, int delta, String eventType, String action,
                String uri, String method, String userAgent, String reason);

    /**
     * 按风险主体计分并执行处置跃迁。
     *
     * @return 处置后该主体的累计风险分
     */
    int addRisk(RiskPrincipal subject, int delta, String eventType, String action,
                String uri, String method, String userAgent, String reason);

    /**
     * 读取主体当前风险分（滑动窗口内）。
     */
    int getRiskScore(String principal);

    /**
     * 读取主体当前风险等级。
     */
    RiskLevel getRiskLevel(String principal);

    /**
     * 判断 IP 是否处于封禁状态。
     */
    boolean isBlocked(String ip);

    /**
     * 判断主体是否处于封禁状态。
     */
    boolean isBlocked(RiskPrincipal subject);

    /**
     * 手动或自动封禁主体。
     *
     * <p>只延长、不缩短：已有封禁的剩余时长更长（或永久）时保持不变，保留原封禁原因。
     * 这样蜜罐等较短时长的写入，不会把最高档封禁或管理员手动的长封禁改短；
     * 需要缩短时请先 {@link #unblockPrincipal(String, boolean)} 再封禁。</p>
     */
    void blockPrincipal(String principal, long durationSeconds, String reason);

    /**
     * 解除主体封禁。
     *
     * @param resetRisk 是否同时清零风险分
     */
    void unblockPrincipal(String principal, boolean resetRisk);

    /**
     * 解除主体封禁并清零风险分（兼容旧调用）。
     */
    default void unblockPrincipal(String principal) {
        unblockPrincipal(principal, true);
    }

    /**
     * 当前处于封禁或存在风险分的全部主体。
     */
    List<RiskSubjectVO> getActiveRiskList();

    /**
     * 风险主体分页查询。
     */
    RiskSubjectPage getRiskSubjectPage(int pageNo, int pageSize, String keyword, Integer level, Boolean blocked);

    /**
     * 查询某个主体最近的安全事件。
     */
    List<SecurityEventDO> getRecentEvents(String principal, int limit);

    /**
     * 匹配爬虫/自动化工具 UA 指纹。
     *
     * @return 命中的指纹，未命中返回 {@code null}
     */
    String matchBlockedUserAgent(String userAgent);

    /**
     * 当前主体是否需要走二次验证（挑战）。
     */
    boolean isChallengeRequired(RiskPrincipal subject);

    /**
     * 标记主体已通过一次风控挑战，短时间内不再重复要求。
     */
    void markChallengePassed(RiskPrincipal subject);
}
