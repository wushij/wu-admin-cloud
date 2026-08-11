package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 系统配置分组保存请求 VO
 */
@Data
public class SysConfigGroupSaveReqVO implements Serializable {

    private Long id;
    private String groupCode;
    private String groupName;
    private String configValue;
    private String remark;
    private Integer status;
}
