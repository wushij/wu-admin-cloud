package com.wu.cloud.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.support.LoginLockSupport;
import com.wu.cloud.system.vo.UserResetPwdReqVO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserServiceImpl 扩展单测")
class SysUserServiceImplMoreTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private LoginLockSupport loginLockSupport;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Test
    @DisplayName("updateUserStatus：用户不存在 404")
    void updateUserStatus_missing_throws() {
        when(sysUserMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.updateUserStatus(9L, 0));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("updateUserStatus：更新状态")
    void updateUserStatus_ok() {
        SysUserDO user = user(1L, "alice");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        sysUserService.updateUserStatus(1L, 0);
        assertEquals(0, user.getStatus());
        verify(sysUserMapper).updateById(user);
    }

    @Test
    @DisplayName("resetPassword：参数不完整拒绝")
    void resetPassword_incomplete_throws() {
        UserResetPwdReqVO req = new UserResetPwdReqVO();
        req.setUserId(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.resetPassword(req));
        assertTrue(ex.getMessage().contains("不完整"));
    }

    @Test
    @DisplayName("resetPassword：加密后更新")
    void resetPassword_hashesAndUpdates() {
        SysUserDO user = user(1L, "alice");
        user.setPassword("old");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        UserResetPwdReqVO req = new UserResetPwdReqVO();
        req.setUserId(1L);
        req.setNewPassword("NewPass1");
        sysUserService.resetPassword(req);

        assertNotEquals("NewPass1", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2"));
        verify(sysUserMapper).updateById(user);
    }

    @Test
    @DisplayName("unlockLogin：用户不存在 404")
    void unlockLogin_missing_throws() {
        when(sysUserMapper.selectById(8L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.unlockLogin(8L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("unlockLogin：成功清锁")
    void unlockLogin_ok() {
        when(sysUserMapper.selectById(1L)).thenReturn(user(1L, "alice"));
        sysUserService.unlockLogin(1L);
        verify(loginLockSupport).unlockUser("alice");
    }

    @Test
    @DisplayName("kickOut：用户不存在 404")
    void kickOut_missing_throws() {
        when(sysUserMapper.selectById(7L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.kickOut(7L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("kickOut：调用 Sa-Token kickout")
    void kickOut_ok() {
        when(sysUserMapper.selectById(1L)).thenReturn(user(1L, "alice"));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            sysUserService.kickOut(1L);
            stp.verify(() -> StpUtil.kickout(1L));
        }
    }

    private static SysUserDO user(Long id, String username) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setUsername(username);
        return u;
    }
}
