package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志 DO 实体
 */
@Data
@TableName("infra_oper_log")
public class OperLogDO implements Serializable {

    @TableId
    private Long id;
    private String title;
    private String businessType;
    private String method;
    private String requestMethod;
    private String operatorType;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
