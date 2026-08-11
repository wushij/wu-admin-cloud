package com.wu.cloud.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分组 DO（对齐单体 sys_file_group 表结构）
 */
@Data
@TableName("sys_file_group")
public class SysFileGroupDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sort;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 分组内文件数量（侧边栏展示，非表字段） */
    @TableField(exist = false)
    private Integer fileCount;
}
