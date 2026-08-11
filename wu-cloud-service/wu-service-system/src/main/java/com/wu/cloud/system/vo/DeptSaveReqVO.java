package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 部门保存请求 VO
 */
@Data
public class DeptSaveReqVO implements Serializable {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
