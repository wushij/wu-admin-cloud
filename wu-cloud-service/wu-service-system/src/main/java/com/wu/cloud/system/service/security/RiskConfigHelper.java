package com.wu.cloud.system.service.security;

import com.wu.cloud.common.config.DynamicConfigHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 风控配置读取器。
 *
 * <p>风控的全部阈值与开关都来自 {@code sys_config_group} 的 {@code security} 分组，
 * 由运营在配置页调整。此处把 DynamicConfigHelper 的通用取值收敛成具名方法，
 * 避免在业务代码里散落魔法字符串。</p>
 *
 * <p>阈值之间有大小约束（如 warning &lt; challenge &lt; block &lt; ban），
 * 读取时统一做钳制，防止运营把某个阈值配得过大导致所有阈值同时失效。</p>
 */
@Component
public class RiskConfigHelper {

    /** 安全配置分组编码 */
    public static final String GROUP_SECURITY = "security";

    /** 告警去重时间（秒） */
    private static final int ALERT_DEDUP_SECONDS = 300;

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    /**
     * 风控总开关。关闭后不再记录与计分（历史风险分不再触发挑战）。
     */
    public boolean isRiskEnabled() {
        return dynamicConfigHelper.getBool(GROUP_SECURITY, "riskEnabled", true);
    }

    /**
     * 风险分滑动窗口长度（分钟），范围 5～1440。
     */
    public int getRiskWindowMinutes() {
        int minutes = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskWindowMinutes", 60);
        return Math.max(5, Math.min(minutes, 1440));
    }

    /**
     * 关注阈值（分）。
     */
    public int getRiskNoticeScore() {
        int score = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskNoticeScore", 30);
        return Math.max(10, Math.min(score, 2000));
    }

    /**
     * 警告阈值（分），必须大于关注阈值。
     */
    public int getRiskWarnScore() {
        int score = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskWarnScore", 50);
        return Math.max(getRiskNoticeScore() + 1, Math.min(score, 2000));
    }

    /**
     * 挑战阈值（分），必须大于警告阈值。
     */
    public int getRiskChallengeScore() {
        int score = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskChallengeScore", 70);
        return Math.max(getRiskWarnScore() + 1, Math.min(score, 2000));
    }

    /**
     * 临时封禁阈值（分），必须大于挑战阈值。
     */
    public int getRiskBlockScore() {
        int score = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskBlockScore", 90);
        return Math.max(getRiskChallengeScore() + 1, Math.min(score, 2000));
    }

    /**
     * 最高档封禁阈值（分），必须大于临时封禁阈值。
     */
    public int getRiskBanScore() {
        int score = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskBanScore", 120);
        return Math.max(getRiskBlockScore() + 1, Math.min(score, 2000));
    }

    /**
     * 临时封禁时长（秒），范围 60～604800。
     */
    public long getRiskBlockSeconds() {
        long seconds = dynamicConfigHelper.getLong(GROUP_SECURITY, "riskBlockSeconds", 3600L);
        return Math.max(60L, Math.min(seconds, 604800L));
    }

    /**
     * 最高档封禁时长（秒），必须不短于临时封禁，且不超过 30 天。
     */
    public long getRiskBanSeconds() {
        long seconds = dynamicConfigHelper.getLong(GROUP_SECURITY, "riskBanSeconds", 86400L);
        return Math.max(getRiskBlockSeconds(), Math.min(seconds, 2592000L));
    }

    /**
     * 是否要求风控挑战（二次验证）。
     */
    public boolean isRiskChallengeEnabled() {
        return dynamicConfigHelper.getBool(GROUP_SECURITY, "riskChallengeEnabled", false);
    }

    /**
     * 风险告警 Webhook 地址，空表示不推送。
     */
    public String getRiskAlertWebhook() {
        return dynamicConfigHelper.getStr(GROUP_SECURITY, "riskAlertWebhook", "");
    }

    /**
     * 是否启用行为识别（页面扫描 / 批量导出）。
     */
    public boolean isRiskBehaviorEnabled() {
        return dynamicConfigHelper.getBool(GROUP_SECURITY, "riskBehaviorEnabled", true);
    }

    /**
     * 页面扫描次数阈值，范围 3～200。
     */
    public int getRiskPageScanThreshold() {
        int threshold = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskPageScanThreshold", 8);
        return Math.max(3, Math.min(threshold, 200));
    }

    /**
     * 批量导出次数阈值，范围 2～100。
     */
    public int getRiskExportThreshold() {
        int threshold = dynamicConfigHelper.getInt(GROUP_SECURITY, "riskExportThreshold", 5);
        return Math.max(2, Math.min(threshold, 100));
    }

    /**
     * 是否启用蜜罐诱捕。
     */
    public boolean isHoneypotEnabled() {
        return dynamicConfigHelper.getBool(GROUP_SECURITY, "honeypotEnabled", false);
    }

    /**
     * 蜜罐命中风险分增量，范围 20～500。
     */
    public int getHoneypotRiskDelta() {
        int delta = dynamicConfigHelper.getInt(GROUP_SECURITY, "honeypotRiskDelta", 80);
        return Math.max(20, Math.min(delta, 500));
    }

    /**
     * 蜜罐命中封禁时长（秒），范围 60～604800。
     */
    public long getHoneypotBlockSeconds() {
        long seconds = dynamicConfigHelper.getLong(GROUP_SECURITY, "honeypotBlockSeconds", 86400L);
        return Math.max(60L, Math.min(seconds, 604800L));
    }

    /**
     * 告警去重时间（秒）。
     */
    public int getAlertDedupSeconds() {
        return ALERT_DEDUP_SECONDS;
    }
}
