package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config_group")
public class SysConfigGroupDO extends BaseEntity {

    private String groupCode;
    private String groupName;
    private String configValue;
    private String remark;
}
