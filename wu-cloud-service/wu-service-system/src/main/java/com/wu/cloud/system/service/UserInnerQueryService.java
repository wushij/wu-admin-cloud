package com.wu.cloud.system.service;

import com.wu.cloud.api.system.dto.AiUserContextDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;

import java.util.List;

/**
 * 用户内部查询服务：为其他微服务提供用户维度的高频查询能力
 * （启用用户列表、批量查询、AI 用户上下文聚合），是用户数据对外输出的唯一口径。
 */
public interface UserInnerQueryService {

    /**
     * 启用状态用户列表
     *
     * @param excludeUserId 需要排除的用户主键（如当前登录用户），可为 null
     */
    List<SysUserDTO> listActiveUsers(Long excludeUserId);

    /**
     * 按主键集合批量查询用户基础信息
     */
    List<SysUserDTO> listUsersByIds(List<Long> ids);

    /**
     * AI 用户上下文聚合：档案 + 部门 + 角色 + 可见菜单，一次返回
     */
    AiUserContextDTO getAiContext(Long userId);
}
