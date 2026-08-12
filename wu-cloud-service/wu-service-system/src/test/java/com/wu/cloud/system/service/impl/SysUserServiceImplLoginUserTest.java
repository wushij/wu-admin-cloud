package com.wu.cloud.system.service.impl;

import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.mapper.SysUserPostMapper;
import com.wu.cloud.system.mapper.SysUserRoleMapper;
import com.wu.cloud.system.testsupport.MybatisLambdaTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.wu.cloud.system.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserServiceImpl 登录用户组装")
class SysUserServiceImplLoginUserTest extends MybatisLambdaTestBase {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysUserPostMapper sysUserPostMapper;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Test
    @DisplayName("空白入参直接返回 null")
    void blank_returnsNull() {
        assertNull(sysUserService.getLoginUserByUsername(" "));
        assertNull(sysUserService.getLoginUserByMobile(null));
        assertNull(sysUserService.getLoginUserByEmail(""));
        verifyNoInteractions(sysUserMapper);
    }

    @Test
    @DisplayName("用户不存在返回 null")
    void missingUser_returnsNull() {
        when(sysUserMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(null);
        assertNull(sysUserService.getLoginUserByUsername("ghost"));
    }

    @Test
    @DisplayName("按用户名组装角色与权限")
    void byUsername_buildsRolesAndPermissions() {
        SysUserDO user = user(1L, "alice");
        when(sysUserMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(user);
        when(sysUserRoleMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("admin", "user"));
        when(sysUserRoleMapper.selectPermissionsByUserId(1L)).thenReturn(List.of("system:user:list"));

        LoginUserDTO dto = sysUserService.getLoginUserByUsername("alice");
        assertNotNull(dto);
        assertEquals(1L, dto.getUserId());
        assertEquals("alice", dto.getUsername());
        assertEquals("secret", dto.getPassword());
        assertTrue(dto.getRoles().contains("admin"));
        assertTrue(dto.getPermissions().contains("system:user:list"));
    }

    @Test
    @DisplayName("按手机号 / 邮箱查询同样组装")
    void byMobileAndEmail() {
        when(sysUserMapper.selectOne(anyLambdaQueryWrapper())).thenReturn(user(2L, "bob"));
        when(sysUserRoleMapper.selectRoleCodesByUserId(2L)).thenReturn(List.of("user"));
        when(sysUserRoleMapper.selectPermissionsByUserId(2L)).thenReturn(List.of());

        assertEquals("bob", sysUserService.getLoginUserByMobile("13800138000").getUsername());
        assertEquals("bob", sysUserService.getLoginUserByEmail("b@c.com").getUsername());
    }

    private static SysUserDO user(Long id, String username) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setPassword("secret");
        u.setStatus(1);
        u.setMobile("13800138000");
        u.setEmail("b@c.com");
        return u;
    }
}
