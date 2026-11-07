package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysRoleMenuDO;
import com.wu.cloud.system.mapper.MenuMapper;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysRoleMenuMapper;
import com.wu.cloud.system.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private MenuMapper menuMapper;

    @Override
    public List<SysRoleDO> getRoleList() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRoleDO>().orderByAsc(SysRoleDO::getSort));
    }

    @Override
    public SysRoleDO getRole(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Long createRole(SysRoleDO roleDO) {
        roleMapper.insert(roleDO);
        return roleDO.getId();
    }

    @Override
    public void updateRole(SysRoleDO roleDO) {
        roleMapper.updateById(roleDO);
    }

    @Override
    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }

    @Override
    public Page<SysRoleDO> pageRoles(PageQuery pageQuery, String name, Integer status) {
        Page<SysRoleDO> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) wrapper.like(SysRoleDO::getName, name);
        if (status != null) wrapper.eq(SysRoleDO::getStatus, status);
        wrapper.orderByAsc(SysRoleDO::getSort);
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public Set<Long> getMenuIdsForAssign(Long roleId) {
        if (roleId == null) {
            return new HashSet<>();
        }
        List<Long> ids = sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        // 关键对齐单体：必须过滤掉父级节点，只向前端返回纯叶子节点 ID！
        // 否则 Element Plus 的 el-tree 在收到父级节点 ID 时，会自动把该父节点下的所有子孙按钮（新增、修改、删除等）全量级联勾选！
        List<SysMenuDO> menus = menuMapper.selectBatchIds(ids);
        if (menus == null || menus.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> parentIdsInAssigned = menus.stream()
                .map(SysMenuDO::getId)
                .filter(id -> menus.stream().anyMatch(m -> id.equals(m.getParentId())))
                .collect(Collectors.toSet());
        return ids.stream()
                .filter(id -> !parentIdsInAssigned.contains(id))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenu(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            throw new BusinessException("角色ID不能为空");
        }
        // 全量覆盖：先清空旧关联，再批量写入新关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        // 勾选按钮时同步补齐父级页面/目录，避免前端因为只有按钮权限而无法在左侧栏展开目录
        Set<Long> expandedMenuIds = expandMenuClosure(new HashSet<>(menuIds));
        List<SysRoleMenuDO> records = new ArrayList<>(expandedMenuIds.size());
        for (Long menuId : expandedMenuIds) {
            SysRoleMenuDO rm = new SysRoleMenuDO();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            records.add(rm);
        }
        sysRoleMenuMapper.insertBatch(records);
    }

    private Set<Long> expandMenuClosure(Set<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<SysMenuDO> allMenus = menuMapper.selectList(null);
        Map<Long, Long> parentMap = new HashMap<>();
        for (SysMenuDO menu : allMenus) {
            if (menu.getParentId() != null && menu.getParentId() > 0) {
                parentMap.put(menu.getId(), menu.getParentId());
            }
        }
        Set<Long> result = new HashSet<>(menuIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(menuIds);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            Long parentId = parentMap.get(id);
            if (parentId != null && parentId > 0 && result.add(parentId)) {
                queue.add(parentId);
            }
        }
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysRoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            throw new BusinessException("角色不存在");
        }
        roleDO.setStatus(status);
        roleMapper.updateById(roleDO);
    }
}
