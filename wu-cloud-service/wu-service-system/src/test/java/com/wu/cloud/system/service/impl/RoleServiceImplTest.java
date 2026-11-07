package com.wu.cloud.system.service.impl;

import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.mapper.MenuMapper;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl 单元测试")
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    /** assignMenu 会读菜单表做父级补齐与叶子过滤，需提供菜单数据源 */
    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("createRole：插入并返回 id")
    void createRole_inserts() {
        SysRoleDO role = new SysRoleDO();
        when(roleMapper.insert(role)).thenAnswer(inv -> {
            role.setId(10L);
            return 1;
        });

        assertEquals(10L, roleService.createRole(role));
        verify(roleMapper).insert(role);
    }

    @Test
    @DisplayName("assignMenu：roleId 为空拒绝")
    void assignMenu_nullRoleId_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleService.assignMenu(null, List.of(1L)));
        assertEquals("角色ID不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("assignMenu：清空旧关联后批量写入")
    void assignMenu_replacesBindings() {
        // 11/12 为叶子按钮且无子节点：assignMenu 只补父级，不额外增加条目
        when(menuMapper.selectList(any())).thenReturn(List.of(
                menu(11L, 0L), menu(12L, 0L)));

        roleService.assignMenu(3L, List.of(11L, 12L));

        verify(sysRoleMenuMapper).deleteByRoleId(3L);
        verify(sysRoleMenuMapper).insertBatch(argThat(list ->
                list.size() == 2
                        && list.get(0).getRoleId().equals(3L)
                        && list.get(0).getMenuId().equals(11L)
                        && list.get(1).getRoleId().equals(3L)
                        && list.get(1).getMenuId().equals(12L)));
    }

    @Test
    @DisplayName("assignMenu：勾选按钮时自动补齐父级菜单")
    void assignMenu_expandsParentMenus() {
        // 11 是 5 的子按钮，5 是 1 的子目录：应一并写入 5 与 1
        when(menuMapper.selectList(any())).thenReturn(List.of(
                menu(11L, 5L), menu(5L, 1L), menu(1L, 0L)));

        roleService.assignMenu(3L, List.of(11L));

        verify(sysRoleMenuMapper).insertBatch(argThat(list ->
                list.size() == 3
                        && list.stream().allMatch(r -> r.getRoleId().equals(3L))
                        && list.stream().anyMatch(r -> r.getMenuId().equals(11L))
                        && list.stream().anyMatch(r -> r.getMenuId().equals(5L))
                        && list.stream().anyMatch(r -> r.getMenuId().equals(1L))));
    }

    private SysMenuDO menu(Long id, Long parentId) {
        SysMenuDO m = new SysMenuDO();
        m.setId(id);
        m.setParentId(parentId);
        return m;
    }

    @Test
    @DisplayName("assignMenu：空菜单列表仅清空")
    void assignMenu_emptyMenus_onlyDeletes() {
        roleService.assignMenu(3L, List.of());
        verify(sysRoleMenuMapper).deleteByRoleId(3L);
        verify(sysRoleMenuMapper, never()).insertBatch(anyList());
    }

    @Test
    @DisplayName("updateStatus：角色不存在拒绝")
    void updateStatus_missingRole_throws() {
        when(roleMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleService.updateStatus(9L, 0));
        assertEquals("角色不存在", ex.getMessage());
    }

    @Test
    @DisplayName("updateStatus：更新状态字段")
    void updateStatus_success() {
        SysRoleDO role = new SysRoleDO();
        role.setId(9L);
        role.setStatus(1);
        when(roleMapper.selectById(9L)).thenReturn(role);

        roleService.updateStatus(9L, 0);

        assertEquals(0, role.getStatus());
        verify(roleMapper).updateById(eq(role));
    }
}
