package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码生成配置 DO（gen_table 表，随代码生成器寄居 wu_infra 库）
 */
@Data
@TableName("gen_table")
public class CodegenTableDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableName;
    /** 表所属微服务数据库（如 wu_system），用于跨库读取表结构 */
    private String schemaName;
    private String tableComment;
    private String className;
    private String packageName;
    private String moduleName;
    private String businessName;
    private String functionName;
    private String author;
    private String genType;
    private String genPath;
    private String frontType;
    private String formLayout;
    private Long parentMenuId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
