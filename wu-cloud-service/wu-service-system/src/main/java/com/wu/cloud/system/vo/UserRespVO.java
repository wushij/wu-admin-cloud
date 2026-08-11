package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户响应视图 VO
 */
@Data
public class UserRespVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer status;
    private Long deptId;
    private String deptName;
    private Set<Long> roleIds;
    private LocalDateTime createTime;
}
