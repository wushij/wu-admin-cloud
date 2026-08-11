package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件发送记录（映射 sys_email_log 表）
 */
@Data
@TableName("sys_email_log")
public class EmailLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String subject;

    private String content;

    /** verify_code / notice / test */
    private String scene;

    /** smtp / sendgrid / aliyun */
    private String provider;

    /** 1-成功 2-失败 */
    private Integer status;

    private String resultMsg;

    private String ip;

    private LocalDateTime createTime;
}
