package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserCreateReqVO implements Serializable {
    private String username;
    private String nickname;
    private String password;
    private String email;
    private String mobile;
    private Integer sex;
    private String avatar;
    private Integer status;
    private Long deptId;
    private Long roleId;
    private java.util.List<Long> postIds;
    private String remark;
}
