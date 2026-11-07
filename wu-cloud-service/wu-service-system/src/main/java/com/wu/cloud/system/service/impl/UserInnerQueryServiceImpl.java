package com.wu.cloud.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.system.dto.AiUserContextDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.system.convert.SysUserConvert;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.DeptMapper;
import com.wu.cloud.system.mapper.MenuMapper;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysRoleMenuMapper;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.mapper.SysUserRoleMapper;
import com.wu.cloud.system.service.UserInnerQueryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户内部查询服务实现：所有查询仅落在 system 自己的库表上。
 */
@Slf4j
@Service
public class UserInnerQueryServiceImpl implements UserInnerQueryService {

    /** 菜单知识只关注目录与菜单（1 目录 2 菜单），按钮不参与 */
    private static final List<Integer> MENU_TYPES = List.of(1, 2);

    private static final Set<String> SUPER_ADMIN_ROLE_CODES = Set.of("admin", "super_admin", "superadmin");

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysUserDTO> listActiveUsers(Long excludeUserId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getStatus, 1)
                .orderByAsc(SysUserDO::getUsername)
                .orderByAsc(SysUserDO::getId);
        if (excludeUserId != null && excludeUserId > 0) {
            wrapper.ne(SysUserDO::getId, excludeUserId);
        }
        return sysUserMapper.selectList(wrapper).stream()
                .map(SysUserConvert::convertToDTO)
                .toList();
    }

    @Override
    public List<SysUserDTO> listUsersByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return sysUserMapper.selectByIds(ids).stream()
                .sorted(Comparator.comparing(SysUserDO::getId))
                .map(SysUserConvert::convertToDTO)
                .toList();
    }

    @Override
    public AiUserContextDTO getAiContext(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        AiUserContextDTO dto = new AiUserContextDTO();
        dto.setUserId(userId);
        dto.setUsername(user.getUsername());
        dto.setNickname(StrUtil.blankToDefault(user.getNickname(), user.getUsername()));

        if (user.getDeptId() != null && user.getDeptId() > 0) {
            SysDeptDO dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                dto.setDeptName(dept.getName());
            }
        }

        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        applyRoles(dto, userId, roleIds);
        dto.setMenus(loadVisibleMenus(roleIds, dto.isSuperAdmin()));
        return dto;
    }

    private void applyRoles(AiUserContextDTO dto, Long userId, List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            dto.setRoleNames(List.of());
            dto.setRoleCodes(List.of());
            dto.setSuperAdmin(userId == 1L);
            return;
        }
        List<SysRoleDO> roles = roleMapper.selectByIds(roleIds).stream()
                .sorted(Comparator.comparing(r -> r.getSort() == null ? 0 : r.getSort()))
                .toList();
        dto.setRoleNames(roles.stream()
                .map(SysRoleDO::getName)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .toList());
        List<String> codes = roles.stream()
                .map(SysRoleDO::getCode)
                .filter(StrUtil::isNotBlank)
                .map(code -> code.trim().toLowerCase())
                .toList();
        dto.setRoleCodes(codes);
        dto.setSuperAdmin(userId == 1L || codes.stream().anyMatch(SUPER_ADMIN_ROLE_CODES::contains));
    }

    /**
     * 可见菜单：超管返回全部启用目录/菜单，普通用户按角色-菜单授权范围过滤
     */
    private List<SysMenuDTO> loadVisibleMenus(List<Long> roleIds, boolean superAdmin) {
        List<SysMenuDO> menus;
        if (superAdmin) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                    .eq(SysMenuDO::getStatus, 1)
                    .in(SysMenuDO::getType, MENU_TYPES)
                    .orderByAsc(SysMenuDO::getSort)
                    .orderByAsc(SysMenuDO::getId));
        } else {
            if (CollUtil.isEmpty(roleIds)) {
                return List.of();
            }
            Set<Long> menuIds = new LinkedHashSet<>();
            for (Long roleId : roleIds) {
                menuIds.addAll(sysRoleMenuMapper.selectMenuIdsByRoleId(roleId));
            }
            if (menuIds.isEmpty()) {
                return List.of();
            }
            menus = menuMapper.selectByIds(menuIds).stream()
                    .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                    .filter(m -> m.getType() != null && MENU_TYPES.contains(m.getType()))
                    .sorted(Comparator
                            .comparing((SysMenuDO m) -> m.getSort() == null ? 0 : m.getSort())
                            .thenComparing(SysMenuDO::getId))
                    .toList();
        }
        List<SysMenuDTO> result = new ArrayList<>(menus.size());
        for (SysMenuDO menu : menus) {
            if (StrUtil.isBlank(menu.getName())) {
                continue;
            }
            result.add(convertMenu(menu));
        }
        return result;
    }

    private SysMenuDTO convertMenu(SysMenuDO menu) {
        SysMenuDTO dto = new SysMenuDTO();
        dto.setId(menu.getId());
        dto.setName(menu.getName());
        dto.setParentId(menu.getParentId());
        dto.setSort(menu.getSort());
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setType(menu.getType());
        dto.setPermission(menu.getPermission());
        dto.setIcon(menu.getIcon());
        dto.setStatus(menu.getStatus());
        dto.setVisible(menu.getVisible() != null ? menu.getVisible()
                : (menu.getStatus() != null && menu.getStatus() == 1 ? 1 : 0));
        return dto;
    }
}
