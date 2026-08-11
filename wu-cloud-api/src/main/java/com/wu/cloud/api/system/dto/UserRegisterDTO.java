package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册跨服务 DTO
 */
@Data
public class UserRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号 */
    private String username;

    /** 明文密码，由提供方完成加密 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;
}
