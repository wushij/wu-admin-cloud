package com.wu.cloud.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新个人资料请求
 */
@Data
public class ProfileUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称 */
    private String nickname;

    /** 性别：0 男 1 女 2 未知 */
    private Integer sex;

    /** 个人简介 */
    private String remark;
}
