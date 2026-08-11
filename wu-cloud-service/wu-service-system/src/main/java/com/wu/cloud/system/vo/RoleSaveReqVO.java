package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 角色保存请求 VO
 */
@Data
public class RoleSaveReqVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private Integer dataScope;
}
