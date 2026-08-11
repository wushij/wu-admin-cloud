package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信发送记录（映射 sys_sms_log 表）
 */
@Data
@TableName("sys_sms_log")
public class SmsLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String content;

    /** verify_code / notice / marketing */
    private String smsType;

    private String templateId;

    private String templateParams;

    /** aliyun / tencent / console */
    private String provider;

    /** 0-发送中 1-成功 2-失败 */
    private Integer status;

    private String resultMsg;

    private String bizId;

    private LocalDateTime sendTime;

    private Long userId;

    private String bizType;

    private String ip;

    private LocalDateTime createTime;
}
