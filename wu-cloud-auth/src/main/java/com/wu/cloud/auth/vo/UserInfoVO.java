package com.wu.cloud.auth.vo;

import com.wu.cloud.api.system.dto.SysMenuDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 用户信息响应 VO（/auth/info 接口返回）
 *
 * <p>字段与前端 AuthInfo 类型对齐：userId / username / nickname / avatar / roles / permissions / menus</p>
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Set<String> roles;
    private Set<String> permissions;
    private List<SysMenuDTO> menus;
}
