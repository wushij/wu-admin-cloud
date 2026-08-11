package com.wu.cloud.codegen.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 待生成代码的字段描述
 */
@Data
public class GenTableColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库列名，如 create_time */
    private String columnName;

    /** 列注释 */
    private String columnComment;

    /** 数据库类型，如 varchar */
    private String columnType;

    /** Java 属性名，如 createTime */
    private String javaField;

    /** Java 类型，如 String / Long / LocalDateTime */
    private String javaType;

    /** 是否主键 */
    private boolean pk;

    /** 是否自增 */
    private boolean increment;

    /** 是否必填 */
    private boolean required;

    /** 是否为 BaseEntity 已包含的公共字段，生成实体时需跳过 */
    private boolean baseField;

    /** 是否作为列表查询条件 */
    private boolean query;

    /** 查询方式：EQ / LIKE / BETWEEN */
    private String queryType = "EQ";

    /** 是否在列表中展示 */
    private boolean list = true;

    /** 是否在新增/编辑表单中展示 */
    private boolean form = true;

    /**
     * Java 类型的简单名，供模板直接书写字段类型
     * 例如 java.time.LocalDateTime -> LocalDateTime
     */
    public String getSimpleJavaType() {
        if (javaType == null) {
            return null;
        }
        int idx = javaType.lastIndexOf('.');
        return idx < 0 ? javaType : javaType.substring(idx + 1);
    }

    /** 首字母大写的属性名，供模板拼接 getter/setter */
    public String getCapJavaField() {
        if (javaField == null || javaField.isEmpty()) {
            return javaField;
        }
        return Character.toUpperCase(javaField.charAt(0)) + javaField.substring(1);
    }
}
