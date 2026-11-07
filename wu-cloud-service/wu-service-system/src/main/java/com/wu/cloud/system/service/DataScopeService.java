package com.wu.cloud.system.service;

import java.util.Set;

/**
 * 部门级数据权限服务（RBAC DataScope）。
 * <p>
 * 依据角色 {@code sys_role.data_scope} 计算当前登录用户可见的部门集合，
 * 业务查询按此范围过滤，避免「有菜单权限但能看全公司数据」的越权。
 * </p>
 */
public interface DataScopeService {

    /**
     * 解析当前登录用户的数据范围
     */
    UserDataScope resolveCurrent();

    /**
     * 数据范围结果
     *
     * @param userId  当前用户
     * @param all     是否全部数据
     * @param deptIds 可见部门集合；非全部且为空时表示「仅本人」
     */
    record UserDataScope(Long userId, boolean all, Set<Long> deptIds) {

        public static UserDataScope allData(Long userId) {
            return new UserDataScope(userId, true, Set.of());
        }

        /** 仅本人：非全部且无可见部门 */
        public boolean selfOnly() {
            return !all && deptIds.isEmpty();
        }
    }
}
