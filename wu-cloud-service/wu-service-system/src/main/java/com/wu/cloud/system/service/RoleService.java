package com.wu.cloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysRoleDO;

import java.util.List;
import java.util.Set;

public interface RoleService {

    List<SysRoleDO> getRoleList();

    SysRoleDO getRole(Long id);

    Long createRole(SysRoleDO roleDO);

    void updateRole(SysRoleDO roleDO);

    void deleteRole(Long id);

    /**
     * 角色分页（补齐 RBAC 角色 CRUD 链路）
     */
    Page<SysRoleDO> pageRoles(PageQuery pageQuery, String name, Integer status);

    /**
     * 查询角色已分配的菜单 ID 集合（用于角色菜单分配回显）
     */
    Set<Long> getMenuIdsForAssign(Long roleId);

    /**
     * 分配角色菜单（全量覆盖语义）
     */
    void assignMenu(Long roleId, List<Long> menuIds);

    /**
     * 更新角色状态（启用/禁用）
     */
    void updateStatus(Long id, Integer status);
}
