package com.wu.cloud.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色菜单分配请求 VO
 */
@Data
public class AssignMenuReqVO implements Serializable {
    private Long roleId;
    private List<Long> menuIds;
}
