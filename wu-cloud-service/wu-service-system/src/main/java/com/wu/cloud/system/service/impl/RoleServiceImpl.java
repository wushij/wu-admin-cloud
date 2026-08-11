package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysRoleMenuDO;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysRoleMenuMapper;
import com.wu.cloud.system.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

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
        return new HashSet<>(ids);
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
        List<SysRoleMenuDO> records = new ArrayList<>(menuIds.size());
        for (Long menuId : menuIds) {
            SysRoleMenuDO rm = new SysRoleMenuDO();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            records.add(rm);
        }
        sysRoleMenuMapper.insertBatch(records);
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
