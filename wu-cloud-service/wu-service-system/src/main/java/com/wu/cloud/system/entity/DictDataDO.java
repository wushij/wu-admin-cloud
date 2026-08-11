package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据 DO 实体 (映射 sys_dict_data)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class DictDataDO extends BaseEntity {

    private Integer sort;
    private String label;
    private String value;
    private String dictType;
    private Integer status;
    private String colorType;
    private String cssClass;
    private String remark;

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
