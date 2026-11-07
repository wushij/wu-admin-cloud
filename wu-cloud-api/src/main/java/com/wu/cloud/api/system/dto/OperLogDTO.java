package com.wu.cloud.api.system.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 远程日志审计 DTO
 */
@Data
public class OperLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志主键（查询/导出场景回填，写入时无需赋值） */
    private Long id;

    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private Integer operatorType;
    private String operName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime operTime;
}
