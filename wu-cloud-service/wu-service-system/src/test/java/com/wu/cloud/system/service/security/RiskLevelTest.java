package com.wu.cloud.system.service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 风险等级档位单元测试。
 *
 * <p>等级是「处置档位」而非单纯分值区间，阈值顺序一旦配错（例如 challenge &lt; warning），
 * 就会让高风险请求落到低风险处置分支上，因此这里逐档验证边界。</p>
 */
@DisplayName("RiskLevel 单元测试")
class RiskLevelTest {

    @Test
    @DisplayName("低于关注阈值为正常")
    void belowNotice() {
        assertEquals(RiskLevel.NORMAL, RiskLevel.of(0, 30, 50, 70, 90));
        assertEquals(RiskLevel.NORMAL, RiskLevel.of(29, 30, 50, 70, 90));
    }

    @Test
    @DisplayName("达到关注阈值为关注档")
    void atNotice() {
        assertEquals(RiskLevel.NOTICE, RiskLevel.of(30, 30, 50, 70, 90));
    }

    @Test
    @DisplayName("达到警告阈值为警告档")
    void atWarning() {
        assertEquals(RiskLevel.WARNING, RiskLevel.of(50, 30, 50, 70, 90));
    }

    @Test
    @DisplayName("达到挑战阈值为挑战档")
    void atChallenge() {
        assertEquals(RiskLevel.CHALLENGE, RiskLevel.of(70, 30, 50, 70, 90));
    }

    @Test
    @DisplayName("达到封禁阈值为临时封禁档")
    void atBlock() {
        assertEquals(RiskLevel.BLOCK, RiskLevel.of(90, 30, 50, 70, 90));
    }

    @Test
    @DisplayName("达到最高档阈值为最高档封禁")
    void atBan() {
        assertEquals(RiskLevel.BAN, RiskLevel.of(120, 30, 50, 70, 90, 120));
    }

    @Test
    @DisplayName("未达最高档阈值时按四档规则降级")
    void belowBanFallsBack() {
        assertEquals(RiskLevel.BLOCK, RiskLevel.of(100, 30, 50, 70, 90, 120));
    }

    @Test
    @DisplayName("fromCode 支持反查，未知编码回退正常")
    void fromCode() {
        assertEquals(RiskLevel.WARNING, RiskLevel.fromCode(2));
        assertEquals(RiskLevel.BAN, RiskLevel.fromCode(5));
        assertEquals(RiskLevel.NORMAL, RiskLevel.fromCode(99));
    }

    @Test
    @DisplayName("等级编码与文案固定，供前端展示")
    void codeAndText() {
        assertEquals(0, RiskLevel.NORMAL.getCode());
        assertEquals("正常", RiskLevel.NORMAL.getText());
        assertEquals(4, RiskLevel.BLOCK.getCode());
        assertEquals("临时封禁", RiskLevel.BLOCK.getText());
        assertEquals(5, RiskLevel.BAN.getCode());
        assertEquals("最高档封禁", RiskLevel.BAN.getText());
    }
}
