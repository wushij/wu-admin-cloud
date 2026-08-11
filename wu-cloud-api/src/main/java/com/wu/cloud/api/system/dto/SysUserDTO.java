package com.wu.cloud.api.system.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 系统用户跨服务 DTO
 */
@Data
public class SysUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private String status;
    private Long deptId;
    private String deptName;

    public String getNickname() {
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        if (StringUtils.hasText(realName)) {
            return realName;
        }
        return username;
    }
}
