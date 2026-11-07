package com.wu.cloud.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.dto.BindMobileDTO;
import com.wu.cloud.auth.dto.ChangePasswordDTO;
import com.wu.cloud.auth.dto.ProfileUpdateDTO;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.vo.ProfileVO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
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
@DisplayName("ProfileServiceImpl 单元测试")
class ProfileServiceImplTest {

    @Mock
    private RemoteUserService remoteUserService;
    @Mock
    private RemoteLoginLogService remoteLoginLogService;
    @Mock
    private RemoteFileService remoteFileService;
    @Mock
    private VerifyCodeService verifyCodeService;
    @Mock
    private com.wu.cloud.auth.support.PasswordCipherService passwordCipherService;
    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(passwordCipherService.resolve(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("getProfile：映射资料与角色权限")
    void getProfile_ok() {
        when(authProperties.getPasswordMinLength()).thenReturn(6);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserId(1L);
        profile.setUsername("alice");
        profile.setNickname("爱丽丝");
        profile.setMobile("13800138000");
        when(remoteUserService.getUserProfile(1L)).thenReturn(R.ok(profile));

        LoginUserDTO login = new LoginUserDTO();
        login.setRoles(java.util.Set.of("admin"));
        login.setPermissions(java.util.Set.of("system:user:list"));
        when(remoteUserService.getUserInfoByUsername("alice")).thenReturn(R.ok(login));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProfileVO vo = profileService.getProfile();
            assertEquals("alice", vo.getUsername());
            assertEquals("13800138000", vo.getMobile());
            assertEquals(java.util.Set.of("admin"), vo.getRoles());
        }
    }

    @Test
    @DisplayName("updateProfile：空参拒绝，成功转发")
    void updateProfile() {
        assertThrows(BusinessException.class, () -> profileService.updateProfile(null));
        when(remoteUserService.updateProfile(any())).thenReturn(R.ok(Boolean.TRUE));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setNickname("新昵称");
            assertDoesNotThrow(() -> profileService.updateProfile(dto));
            verify(remoteUserService).updateProfile(argThat(u ->
                    Long.valueOf(1L).equals(u.getUserId()) && "新昵称".equals(u.getNickname())));
        }
    }

    @Test
    @DisplayName("changePassword：原密码错误 / 成功后 logout")
    void changePassword() {
        when(authProperties.getPasswordMinLength()).thenReturn(6);
        stubCurrentUser("alice", "oldPass");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            ChangePasswordDTO bad = new ChangePasswordDTO();
            bad.setOldPassword("wrong");
            bad.setNewPassword("newPass1");
            bad.setConfirmPassword("newPass1");
            assertThrows(BusinessException.class, () -> profileService.changePassword(bad));

            when(remoteUserService.resetPassword(eq(1L), anyString())).thenReturn(R.ok(Boolean.TRUE));
            ChangePasswordDTO ok = new ChangePasswordDTO();
            ok.setOldPassword("oldPass");
            ok.setNewPassword("newPass1");
            ok.setConfirmPassword("newPass1");
            assertDoesNotThrow(() -> profileService.changePassword(ok));
            stp.verify(StpUtil::logout);
        }
    }

    @Test
    @DisplayName("sendMobileBindCode / bindMobile：占用冲突与成功")
    void bindMobile_flow() {
        LoginUserDTO other = new LoginUserDTO();
        other.setUserId(99L);
        when(remoteUserService.getUserByMobile("13900139000")).thenReturn(R.ok(other));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> profileService.sendMobileBindCode("13900139000"));
            assertTrue(ex.getMessage().contains("已被其他账号绑定"));

            when(remoteUserService.getUserByMobile("13800138000")).thenReturn(R.ok());
            profileService.sendMobileBindCode("13800138000");
            verify(verifyCodeService).sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000");

            BindMobileDTO dto = new BindMobileDTO();
            dto.setMobile("13800138000");
            dto.setCode("123456");
            when(remoteUserService.bindMobile(1L, "13800138000")).thenReturn(R.ok(Boolean.TRUE));
            assertDoesNotThrow(() -> profileService.bindMobile(dto));
            verify(verifyCodeService).verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE, "13800138000", "123456");
        }
    }

    private void stubCurrentUser(String username, String password) {
        SysUserDTO basic = new SysUserDTO();
        basic.setUsername(username);
        when(remoteUserService.getUserById(1L)).thenReturn(R.ok(basic));
        LoginUserDTO full = new LoginUserDTO();
        full.setUserId(1L);
        full.setUsername(username);
        full.setPassword(password);
        when(remoteUserService.getUserInfoByUsername(username)).thenReturn(R.ok(full));
    }
}
