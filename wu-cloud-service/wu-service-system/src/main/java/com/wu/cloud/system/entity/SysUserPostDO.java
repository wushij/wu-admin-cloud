package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-岗位关联
 */
@Data
@TableName("sys_user_post")
public class SysUserPostDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
}
