package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CaptchaServiceImpl 单元测试")
class CaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AuthProperties authProperties;
    @Mock
    private AuthConfigLoader authConfigLoader;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @Test
    @DisplayName("verifyImageCaptcha：关闭验证码时直接放行")
    void verifyImage_disabled_skips() {
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(false);
        assertDoesNotThrow(() -> captchaService.verifyImageCaptcha(null, null));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("verifyImageCaptcha：空入参拒绝")
    void verifyImage_blank_throws() {
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyImageCaptcha("", "ab"));
        assertEquals("请输入验证码", ex.getMessage());
    }

    @Test
    @DisplayName("verifyImageCaptcha：过期拒绝并删除 key")
    void verifyImage_expired_throws() {
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AuthRedisKeys.captcha("u1"))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyImageCaptcha("u1", "abcd"));
        assertTrue(ex.getMessage().contains("过期"));
        verify(redisTemplate).delete(AuthRedisKeys.captcha("u1"));
    }

    @Test
    @DisplayName("verifyImageCaptcha：错误码拒绝")
    void verifyImage_wrongCode_throws() {
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AuthRedisKeys.captcha("u1"))).thenReturn("abcd");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyImageCaptcha("u1", "zzzz"));
        assertEquals("验证码错误", ex.getMessage());
        verify(redisTemplate).delete(AuthRedisKeys.captcha("u1"));
    }

    @Test
    @DisplayName("verifyImageCaptcha：匹配成功")
    void verifyImage_ok() {
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AuthRedisKeys.captcha("u1"))).thenReturn("ab12");

        assertDoesNotThrow(() -> captchaService.verifyImageCaptcha("u1", "AB12"));
        verify(redisTemplate).delete(AuthRedisKeys.captcha("u1"));
    }

    @Test
    @DisplayName("verifySlider：偏差过大失败")
    void verifySlider_offsetTooLarge() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AuthRedisKeys.slider("tok"))).thenReturn("100");
        when(authProperties.getSliderTolerance()).thenReturn(5);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifySlider("tok", 120));
        assertTrue(ex.getMessage().contains("滑块位置不正确"));
        verify(redisTemplate).delete(AuthRedisKeys.slider("tok"));
    }

    @Test
    @DisplayName("verifySlider：匹配签发一次性票据")
    void verifySlider_ok_issuesTicket() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(AuthRedisKeys.slider("tok"))).thenReturn("100");
        when(authProperties.getSliderTolerance()).thenReturn(5);
        when(authProperties.getCaptchaExpireSeconds()).thenReturn(300L);

        String ticket = captchaService.verifySlider("tok", 103);
        assertNotNull(ticket);
        assertFalse(ticket.isBlank());
        verify(valueOps).set(eq(AuthRedisKeys.sliderPass(ticket)), eq("1"), any());
    }

    @Test
    @DisplayName("verifySliderTicket：缺票拒绝")
    void verifySliderTicket_blank() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifySliderTicket(" "));
        assertTrue(ex.getMessage().contains("滑块验证"));
    }

    @Test
    @DisplayName("verifySliderTicket：失效拒绝")
    void verifySliderTicket_missing() {
        when(redisTemplate.delete(AuthRedisKeys.sliderPass("t1"))).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifySliderTicket("t1"));
        assertTrue(ex.getMessage().contains("失效"));
    }

    @Test
    @DisplayName("verifySliderTicket：成功一次性消费")
    void verifySliderTicket_ok() {
        when(redisTemplate.delete(AuthRedisKeys.sliderPass("t1"))).thenReturn(true);
        assertDoesNotThrow(() -> captchaService.verifySliderTicket("t1"));
    }
}
