package com.wu.cloud.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码生成配置 DO（对齐单体 gen_table 表结构）
 */
@Data
@TableName("gen_table")
public class CodegenTableDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableName;
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
