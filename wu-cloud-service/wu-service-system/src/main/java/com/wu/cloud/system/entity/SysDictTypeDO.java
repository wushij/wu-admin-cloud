package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型（列名 dict_name/dict_type，JSON 对齐前端 dictName/dictType；主键沿用 BaseEntity AUTO）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictTypeDO extends BaseEntity {

    @TableField("dict_name")
    @JsonProperty("dictName")
    @JsonAlias({"name", "dictName"})
    private String name;

    @TableField("dict_type")
    @JsonProperty("dictType")
    @JsonAlias({"type", "dictType"})
    private String type;

    private Integer status;
    private String remark;

    /** 字典数据条数（列表展示，非表字段） */
    @TableField(exist = false)
    private Integer dataCount;
}
