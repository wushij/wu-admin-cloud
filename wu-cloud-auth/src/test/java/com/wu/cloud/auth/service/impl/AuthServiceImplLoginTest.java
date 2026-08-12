package com.wu.cloud.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.UserRegisterDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.dto.LoginQueryDTO;
import com.wu.cloud.auth.dto.RegisterDTO;
import com.wu.cloud.auth.service.CaptchaService;
import com.wu.cloud.auth.service.LoginLockService;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.vo.LoginVO;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl 登录/注册单测")
class AuthServiceImplLoginTest {

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
        when(authConfigLoader.isCaptchaEnabled()).thenReturn(false);
        when(request.getHeader(anyString())).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(loginLockService.recordFailure(anyString())).thenReturn(3);
        when(authConfigLoader.getLockTimeMinutes()).thenReturn(10);
    }

    @Test
    @DisplayName("login：参数为空拒绝")
    void login_nullQuery_throws() {
        assertThrows(BusinessException.class, () -> authService.login(null));
    }

    @Test
    @DisplayName("login：账号已锁定拒绝")
    void login_locked_throws() {
        doThrow(new BusinessException("账号已被锁定，请 2 分钟后重试"))
                .when(loginLockService).assertNotLocked("alice");

        LoginQueryDTO query = passwordLogin("alice", "secret");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(query));
        assertTrue(ex.getMessage().contains("锁定"));
        verify(remoteUserService, never()).getUserInfoByUsername(anyString());
    }

    @Test
    @DisplayName("login：账号不存在返回统一错误并记失败")
    void login_userNotFound_unifiedMessage() {
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(passwordLogin("alice", "secret")));
        assertTrue(ex.getMessage().startsWith("账号或密码错误"));
        verify(loginLockService).recordFailure("alice");
    }

    @Test
    @DisplayName("login：密码错误返回统一提示")
    void login_wrongPassword_unifiedMessage() {
        LoginUserDTO user = user(1L, "alice", "plain-secret", 1);
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(passwordLogin("alice", "wrong")));
        assertTrue(ex.getMessage().startsWith("账号或密码错误"));
        verify(loginLockService).recordFailure("alice");
    }

    @Test
    @DisplayName("login：停用账号拒绝")
    void login_disabled_throws() {
        LoginUserDTO user = user(1L, "alice", "secret", 0);
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(passwordLogin("alice", "secret")));
        assertTrue(ex.getMessage().contains("停用"));
        verify(loginLockService, never()).clearFailure(anyString());
    }

    @Test
    @DisplayName("login：成功登录清失败计数并返回 token")
    void login_success() {
        LoginUserDTO user = user(1L, "alice", "secret", 1);
        user.setNickname("爱丽丝");
        user.setRoles(Set.of("common"));
        user.setPermissions(Set.of("system:user:query"));
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(user));
        when(authConfigLoader.isConcurrentLogin()).thenReturn(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            SaSession session = mock(SaSession.class);
            stp.when(StpUtil::getSession).thenReturn(session);
            stp.when(StpUtil::getTokenValue).thenReturn("tok-1");
            stp.when(StpUtil::getTokenName).thenReturn("Authorization");

            LoginVO vo = authService.login(passwordLogin("alice", "secret"));

            assertEquals(1L, vo.getUserId());
            assertEquals("tok-1", vo.getToken());
            assertEquals("alice", vo.getUsername());
            verify(loginLockService).clearFailure("alice");
            stp.verify(() -> StpUtil.login(1L));
            verify(session).set("username", "alice");
            verify(session).set(eq("roles"), eq(Set.of("common")));
        }
    }

    @Test
    @DisplayName("login SMS：未绑定手机号拒绝")
    void loginSms_unboundMobile_throws() {
        when(authConfigLoader.isSmsLoginEnabled()).thenReturn(true);
        when(authConfigLoader.isSmsEnabled()).thenReturn(true);
        when(remoteUserService.getUserByMobile("13800138000")).thenReturn(R.ok(null));

        LoginQueryDTO query = new LoginQueryDTO();
        query.setLoginType("sms");
        query.setMobile("13800138000");
        query.setSmsCode("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(query));
        assertEquals("该手机号未绑定账号", ex.getMessage());
        verify(verifyCodeService).verifySmsCode(VerifyCodeService.SCENE_SMS_LOGIN, "13800138000", "123456");
    }

    @Test
    @DisplayName("login SMS：功能未开启拒绝")
    void loginSms_disabled_throws() {
        when(authConfigLoader.isSmsLoginEnabled()).thenReturn(false);
        LoginQueryDTO query = new LoginQueryDTO();
        query.setLoginType("sms");
        query.setMobile("13800138000");
        query.setSmsCode("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(query));
        assertEquals("短信登录未开启", ex.getMessage());
    }

    @Test
    @DisplayName("register：用户名已存在拒绝")
    void register_usernameExists() {
        when(authConfigLoader.isRegisterEnabled()).thenReturn(true);
        when(authConfigLoader.isRegisterCaptchaEnabled()).thenReturn(false);
        when(authConfigLoader.getRegisterMinPasswordLength()).thenReturn(6);
        LoginUserDTO existing = user(2L, "bob", "x", 1);
        when(remoteUserService.getUserInfoByUsername("bob")).thenReturn(R.ok(existing));

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("bob");
        dto.setPassword("secret1");
        dto.setConfirmPassword("secret1");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(dto));
        assertEquals("账号已存在", ex.getMessage());
        verify(remoteUserService, never()).registerUser(any(UserRegisterDTO.class));
    }

    @Test
    @DisplayName("register：成功调用远程注册")
    void register_success() {
        when(authConfigLoader.isRegisterEnabled()).thenReturn(true);
        when(authConfigLoader.isRegisterCaptchaEnabled()).thenReturn(false);
        when(authConfigLoader.getRegisterMinPasswordLength()).thenReturn(6);
        when(remoteUserService.getUserInfoByUsername("bob")).thenReturn(R.ok(null));
        when(remoteUserService.registerUser(any(UserRegisterDTO.class))).thenReturn(R.ok(99L));

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("bob");
        dto.setPassword("secret1");
        dto.setConfirmPassword("secret1");

        assertEquals(99L, authService.register(dto));
        verify(remoteUserService).registerUser(argThat(r ->
                "bob".equals(r.getUsername()) && r.getPassword() != null));
    }

    private static LoginQueryDTO passwordLogin(String username, String password) {
        LoginQueryDTO q = new LoginQueryDTO();
        q.setLoginType("password");
        q.setUsername(username);
        q.setPassword(password);
        return q;
    }

    private static LoginUserDTO user(Long id, String username, String password, int status) {
        LoginUserDTO u = new LoginUserDTO();
        u.setUserId(id);
        u.setUsername(username);
        u.setPassword(password);
        u.setStatus(status);
        return u;
    }
}
