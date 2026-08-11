package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据（列名 dict_label/dict_value，JSON 对齐前端 dictLabel/dictValue；主键沿用 BaseEntity AUTO）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictDataDO extends BaseEntity {

    private Integer sort;

    @TableField("dict_label")
    @JsonProperty("dictLabel")
    @JsonAlias({"label", "dictLabel"})
    private String label;

    @TableField("dict_value")
    @JsonProperty("dictValue")
    @JsonAlias({"value", "dictValue"})
    private String value;

    private String dictType;
    private Integer status;

    @TableField("list_class")
    @JsonProperty("listClass")
    @JsonAlias({"colorType", "listClass"})
    private String colorType;

    @TableField("css_class")
    private String cssClass;

    @TableField("is_default")
    private Integer isDefault;

    private String remark;
}
