package com.wu.cloud.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.dto.ForgotPasswordCheckDTO;
import com.wu.cloud.auth.dto.ForgotPasswordResetDTO;
import com.wu.cloud.auth.dto.ForgotPasswordSmsCodeDTO;
import com.wu.cloud.auth.service.CaptchaService;
import com.wu.cloud.auth.service.LoginLockService;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.vo.ForgotPasswordCheckVO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl 找回密码单测")
class AuthServiceImplForgotPasswordTest {

    @Mock
    private RemoteUserService remoteUserService;
    @Mock
    private RemoteLoginLogService remoteLoginLogService;
    @Mock
    private RemoteOnlineUserService remoteOnlineUserService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private VerifyCodeService verifyCodeService;
    @Mock
    private LoginLockService loginLockService;
    @Mock
    private AuthConfigLoader authConfigLoader;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(authConfigLoader.getRegisterMinPasswordLength()).thenReturn(6);
    }

    @Test
    @DisplayName("forgotPasswordCheck：账号不存在拒绝")
    void check_userNotFound() {
        when(remoteUserService.getUserInfoByUsername("ghost")).thenReturn(R.ok(null));

        ForgotPasswordCheckDTO dto = new ForgotPasswordCheckDTO();
        dto.setUsername("ghost");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.forgotPasswordCheck(dto));
        assertEquals("账号不存在", ex.getMessage());
    }

    @Test
    @DisplayName("forgotPasswordCheck：未绑定手机拒绝")
    void check_noMobile() {
        LoginUserDTO user = user(1L, "alice", null);
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));

        ForgotPasswordCheckDTO dto = new ForgotPasswordCheckDTO();
        dto.setUsername("alice");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.forgotPasswordCheck(dto));
        assertTrue(ex.getMessage().contains("未绑定手机号"));
    }

    @Test
    @DisplayName("forgotPasswordCheck：返回脱敏手机号")
    void check_returnsMaskedMobile() {
        LoginUserDTO user = user(1L, "alice", "13800138000");
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));

        ForgotPasswordCheckDTO dto = new ForgotPasswordCheckDTO();
        dto.setUsername("alice");
        ForgotPasswordCheckVO vo = authService.forgotPasswordCheck(dto);

        assertTrue(Boolean.TRUE.equals(vo.getExists()));
        assertNotNull(vo.getMaskedMobile());
        assertTrue(vo.getMaskedMobile().contains("*"));
        assertFalse(vo.getMaskedMobile().contains("13800138000"));
    }

    @Test
    @DisplayName("forgotPasswordSmsCode：向绑定手机发码")
    void smsCode_sendsToBoundMobile() {
        LoginUserDTO user = user(1L, "alice", "13800138000");
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));

        ForgotPasswordSmsCodeDTO dto = new ForgotPasswordSmsCodeDTO();
        dto.setUsername("alice");
        String masked = authService.forgotPasswordSmsCode(dto);

        assertTrue(masked.contains("*"));
        verify(verifyCodeService).sendSmsCode(VerifyCodeService.SCENE_FORGOT_PASSWORD, "13800138000");
    }

    @Test
    @DisplayName("forgotPasswordReset：校验验证码并重置密码")
    void reset_success() {
        LoginUserDTO user = user(1L, "alice", "13800138000");
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));
        when(remoteUserService.resetPassword(eq(1L), anyString())).thenReturn(R.ok(true));

        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setUsername("alice");
        dto.setCode("123456");
        dto.setNewPassword("newpass1");
        dto.setConfirmPassword("newpass1");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            assertDoesNotThrow(() -> authService.forgotPasswordReset(dto));
            stp.verify(() -> StpUtil.logout(1L));
        }

        verify(verifyCodeService).verifySmsCode(VerifyCodeService.SCENE_FORGOT_PASSWORD, "13800138000", "123456");
        verify(remoteUserService).resetPassword(eq(1L), anyString());
        verify(loginLockService).clearFailure("alice");
    }

    @Test
    @DisplayName("forgotPasswordReset：远程重置失败向上抛出")
    void reset_remoteFail_throws() {
        LoginUserDTO user = user(1L, "alice", "13800138000");
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));
        when(remoteUserService.resetPassword(eq(1L), anyString())).thenReturn(R.fail("下游失败"));

        ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
        dto.setUsername("alice");
        dto.setCode("123456");
        dto.setNewPassword("newpass1");
        dto.setConfirmPassword("newpass1");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.forgotPasswordReset(dto));
        assertEquals("下游失败", ex.getMessage());
    }

    private static LoginUserDTO user(Long id, String username, String mobile) {
        LoginUserDTO u = new LoginUserDTO();
        u.setUserId(id);
        u.setUsername(username);
        u.setMobile(mobile);
        return u;
    }
}
