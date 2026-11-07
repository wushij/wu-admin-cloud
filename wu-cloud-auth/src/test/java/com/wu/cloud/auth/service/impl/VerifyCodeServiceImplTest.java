package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.mail.MailResult;
import com.wu.cloud.common.mail.MailSender;
import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VerifyCodeServiceImpl 单元测试")
class VerifyCodeServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AuthProperties authProperties;
    @Mock
    private AuthConfigLoader authConfigLoader;
    @Mock
    private com.wu.cloud.common.sms.SmsSenderFactory smsSenderFactory;
    @Mock
    private com.wu.cloud.api.infra.feign.RemoteConfigLogService remoteConfigLogService;
    @Mock
    private SmsSender smsSender;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private VerifyCodeServiceImpl verifyCodeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(authProperties.getCodeExpireSeconds()).thenReturn(300L);
        when(authConfigLoader.getSmsSendIntervalSeconds()).thenReturn(60);
        when(redisTemplate.getExpire(anyString())).thenReturn(-2L);
        when(smsSenderFactory.sender()).thenReturn(smsSender);
    }

    @Test
    @DisplayName("sendSmsCode：非法手机号拒绝")
    void sendSms_invalidMobile() {
        assertThrows(BusinessException.class,
                () -> verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "123"));
    }

    @Test
    @DisplayName("sendSmsCode：发送过频拒绝")
    void sendSms_tooFrequent() {
        when(redisTemplate.getExpire(AuthRedisKeys.codeInterval(
                VerifyCodeService.SCENE_BIND_MOBILE, "13800138000"))).thenReturn(30L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000"));
        assertTrue(ex.getMessage().contains("发送过于频繁"));
    }

    @Test
    @DisplayName("sendSmsCode：成功写入验证码并标记间隔")
    void sendSms_success() {
        when(smsSender.send(eq("13800138000"), eq(VerifyCodeService.SCENE_BIND_MOBILE), anyMap()))
                .thenReturn(SmsResult.success("biz"));

        verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000");

        verify(valueOps).set(eq(AuthRedisKeys.smsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000")),
                argThat(code -> code != null && code.length() == 6),
                eq(Duration.ofSeconds(300L)));
        verify(valueOps).set(eq(AuthRedisKeys.codeInterval(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000")),
                eq("1"), eq(Duration.ofSeconds(60L)));
    }

    @Test
    @DisplayName("sendSmsCode：发送失败清理并抛错")
    void sendSms_fail_cleansKeys() {
        when(smsSender.send(anyString(), anyString(), ArgumentMatchers.<Map<String, String>>any()))
                .thenReturn(SmsResult.fail("E", "通道异常"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000"));
        assertTrue(ex.getMessage().contains("短信发送失败"));
        verify(redisTemplate).delete(AuthRedisKeys.smsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000"));
        verify(redisTemplate).delete(AuthRedisKeys.codeInterval(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000"));
    }

    @Test
    @DisplayName("sendEmailCode：成功路径")
    void sendEmail_success() {
        when(mailSender.send(eq("a@b.com"), anyString(), anyString())).thenReturn(MailResult.success());
        verifyCodeService.sendEmailCode(VerifyCodeService.SCENE_BIND_EMAIL, "a@b.com");
        verify(valueOps).set(eq(AuthRedisKeys.emailCode(VerifyCodeService.SCENE_BIND_EMAIL, "a@b.com")),
                anyString(), eq(Duration.ofSeconds(300L)));
    }

    @Test
    @DisplayName("verifySmsCode：过期 / 错误 / 成功")
    void verifySms_branches() {
        String key = AuthRedisKeys.smsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000");
        assertThrows(BusinessException.class,
                () -> verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000", " "));

        when(valueOps.get(key)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000", "123456"));

        when(valueOps.get(key)).thenReturn("123456");
        assertThrows(BusinessException.class,
                () -> verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000", "000000"));

        when(valueOps.get(key)).thenReturn("123456");
        assertDoesNotThrow(() ->
                verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000", "123456"));
        verify(redisTemplate, atLeastOnce()).delete(key);
    }

    @Test
    @DisplayName("verifyAndIssueTicket + consumeTicket")
    void ticket_issueAndConsume() {
        String smsKey = AuthRedisKeys.smsCode(VerifyCodeService.SCENE_FORGOT_PASSWORD, "13800138000");
        when(valueOps.get(smsKey)).thenReturn("654321");

        String ticket = verifyCodeService.verifyAndIssueTicket(
                VerifyCodeService.SCENE_FORGOT_PASSWORD, "13800138000", "654321", true);
        assertNotNull(ticket);
        assertFalse(ticket.isBlank());

        ArgumentCaptor<String> ticketKey = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(ticketKey.capture(), eq("13800138000"), eq(Duration.ofSeconds(300L)));

        when(valueOps.get(ticketKey.getValue())).thenReturn("13800138000");
        assertEquals("13800138000",
                verifyCodeService.consumeTicket(VerifyCodeService.SCENE_FORGOT_PASSWORD, ticket));
        verify(redisTemplate).delete(ticketKey.getValue());
    }

    @Test
    @DisplayName("consumeTicket：空 / 失效拒绝")
    void consumeTicket_invalid() {
        assertThrows(BusinessException.class,
                () -> verifyCodeService.consumeTicket(VerifyCodeService.SCENE_FORGOT_PASSWORD, " "));
        when(valueOps.get(anyString())).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> verifyCodeService.consumeTicket(VerifyCodeService.SCENE_FORGOT_PASSWORD, "dead"));
    }
}
