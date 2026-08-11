package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户个人资料更新跨服务 DTO
 */
@Data
public class UserProfileUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主键 */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 性别：0 男 1 女 2 未知 */
    private Integer sex;

    /** 备注/简介 */
    private String remark;
}
