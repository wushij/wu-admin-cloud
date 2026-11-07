package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 用户上下文聚合 DTO：一次 RPC 返回提示词组装所需的用户档案、部门、角色与可见菜单，
 * 避免 AI 服务逐表跨库查询或多次远程调用。
 */
@Data
public class AiUserContextDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String deptName;
    private List<String> roleNames;
    private List<String> roleCodes;
    private boolean superAdmin;
    /** 当前用户可见的启用菜单（type=1/2，已按 sort 排序） */
    private List<SysMenuDTO> menus;
}
