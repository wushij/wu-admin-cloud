package com.wu.cloud.system.service.impl;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.mapper.SysUserRoleMapper;
import com.wu.cloud.system.service.DataScopeService;
import com.wu.cloud.system.service.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 部门级数据权限实现。
 * <p>
 * 规则（多角色取并集，任一角色为「全部」则全部可见）：
 * <ul>
 *   <li>超级管理员（userId=1）→ 全部</li>
 *   <li>1 全部 / 2 自定义部门 / 3 本部门 / 4 本部门及以下 / 5 仅本人</li>
 *   <li>无角色 → 仅本人；无登录上下文 → 不额外限制（服务内部调用）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class DataScopeServiceImpl implements DataScopeService {

    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_CUSTOM = 2;
    private static final int SCOPE_DEPT = 3;
    private static final int SCOPE_DEPT_AND_CHILD = 4;
    private static final int SCOPE_SELF = 5;

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private DeptService deptService;

    @Override
    public UserDataScope resolveCurrent() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || userId <= 0) {
            // 无登录上下文：服务内部调用（如定时任务、启动预热），不做额外限制
            return UserDataScope.allData(null);
        }
        if (userId == 1L) {
            return UserDataScope.allData(userId);
        }
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            return new UserDataScope(userId, false, Set.of());
        }

        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new UserDataScope(userId, false, Set.of());
        }
        List<SysRoleDO> roles = roleMapper.selectByIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return new UserDataScope(userId, false, Set.of());
        }

        Set<Long> deptIds = new HashSet<>();
        boolean selfOnly = false;
        for (SysRoleDO role : roles) {
            Integer scope = role.getDataScope();
            if (scope == null || scope == SCOPE_ALL) {
                return UserDataScope.allData(userId);
            }
            switch (scope) {
                case SCOPE_CUSTOM -> deptIds.addAll(parseDeptIds(role.getDataScopeDeptIds()));
                case SCOPE_DEPT -> {
                    if (user.getDeptId() != null && user.getDeptId() > 0) {
                        deptIds.add(user.getDeptId());
                    }
                }
                case SCOPE_DEPT_AND_CHILD -> {
                    if (user.getDeptId() != null && user.getDeptId() > 0) {
                        deptIds.add(user.getDeptId());
                        deptIds.addAll(deptService.listSelfAndDescendantIds(user.getDeptId()));
                    }
                }
                case SCOPE_SELF -> selfOnly = true;
                default -> {
                    // 未知取值按「全部」处理，与既有历史数据语义保持一致
                    return UserDataScope.allData(userId);
                }
            }
        }
        if (selfOnly && deptIds.isEmpty()) {
            return new UserDataScope(userId, false, Set.of());
        }
        return new UserDataScope(userId, false, deptIds);
    }

    private Set<Long> parseDeptIds(String csv) {
        Set<Long> result = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                log.warn("[DataScope] 非法的自定义部门 ID: {}", trimmed);
            }
        }
        return result;
    }
}
