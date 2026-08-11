package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录成功响应视图 VO
 *
 * <p>字段布局与单体 completeLogin 返回值保持一致，userId/username/nickname/token 均在顶层，
 * 便于前端 loginAction 直接读取 res.data.userId / res.data.token 而无需嵌套取值。</p>
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Sa-Token Token 值 */
    private String token;

    /** Sa-Token Token 名称（Authorization） */
    private String tokenName;

    /** 用户主键（顶层，与单体对齐） */
    private Long userId;

    /** 账号（顶层，与单体对齐） */
    private String username;

    /** 昵称（顶层，与单体对齐） */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 权限标识集合 */
    private Set<String> permissions;
}
