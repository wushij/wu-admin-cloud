package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API 访问日志 DO 实体
 */
@Data
@TableName("infra_access_log")
public class AccessLogDO implements Serializable {

    @TableId
    private Long id;
    private String traceId;
    private String userId;
    private String userName;
    private String applicationName;
    private String requestUrl;
    private String requestMethod;
    private String userAgent;
    private String remoteAddr;
    private String serverAddr;
    private Integer status;
    private Long duration;
    private LocalDateTime accessTime;
}
