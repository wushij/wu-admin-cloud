package com.wu.cloud.system.service.impl;

import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.mapper.SysUserPostMapper;
import com.wu.cloud.system.mapper.SysUserRoleMapper;
import com.wu.cloud.system.testsupport.MybatisLambdaTestBase;
import com.wu.cloud.system.vo.UserCreateReqVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.wu.cloud.system.testsupport.MybatisMockMatchers.anyLambdaQueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserServiceImpl 单元测试")
class SysUserServiceImplTest extends MybatisLambdaTestBase {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysUserPostMapper sysUserPostMapper;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @Test
    @DisplayName("getUserById：透传 Mapper")
    void getUserById_returnsMapperResult() {
        SysUserDO user = new SysUserDO();
        user.setId(1L);
        user.setUsername("admin");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        assertSame(user, sysUserService.getUserById(1L));
    }

    @Test
    @DisplayName("createUser：密码为空拒绝")
    void createUser_blankPassword_throws() {
        UserCreateReqVO req = new UserCreateReqVO();
        req.setUsername("alice");
        req.setNickname("爱丽丝");
        req.setPassword(" ");

        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.createUser(req));
        assertEquals("密码不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("createUser：用户名格式非法拒绝")
    void createUser_invalidUsername_throws() {
        UserCreateReqVO req = new UserCreateReqVO();
        req.setUsername("ab");
        req.setNickname("短名");
        req.setPassword("Passw0rd!");

        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.createUser(req));
        assertTrue(ex.getMessage().contains("用户名须为"));
    }

    @Test
    @DisplayName("createUser：账号已存在拒绝")
    void createUser_duplicateUsername_throws() {
        UserCreateReqVO req = new UserCreateReqVO();
        req.setUsername("alice");
        req.setNickname("爱丽丝");
        req.setPassword("Passw0rd!");
        when(sysUserMapper.selectCount(anyLambdaQueryWrapper())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> sysUserService.createUser(req));
        assertEquals("账号已存在", ex.getMessage());
    }

    @Test
    @DisplayName("createUser：成功入库并返回 id")
    void createUser_success_inserts() {
        UserCreateReqVO req = new UserCreateReqVO();
        req.setUsername("alice");
        req.setNickname("爱丽丝");
        req.setPassword("Passw0rd!");
        when(sysUserMapper.selectCount(anyLambdaQueryWrapper())).thenReturn(0L);
        when(sysUserMapper.insert(any(SysUserDO.class))).thenAnswer(inv -> {
            SysUserDO u = inv.getArgument(0);
            u.setId(88L);
            return 1;
        });

        Long id = sysUserService.createUser(req);
        assertEquals(88L, id);
        verify(sysUserMapper).insert(ArgumentMatchers.<SysUserDO>argThat(u ->
                "alice".equals(u.getUsername())
                        && "爱丽丝".equals(u.getNickname())
                        && u.getPassword() != null
                        && !req.getPassword().equals(u.getPassword())));
        verify(sysUserPostMapper).deleteByUserId(88L);
    }
}
