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
    /** 自定义数据范围部门 ID（逗号分隔） */
    private String dataScopeDeptIds;
}
