package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SysUserRoleReqVO implements Serializable {
    private Long userId;
    private List<Long> roleIds;
}
