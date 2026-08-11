package com.wu.cloud.api.file.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息 DTO (跨服务 RPC 传输)
 */
@Data
public class SysFileDTO implements Serializable {

    private Long id;
    private String originalName;
    private String fileName;
    private String url;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createTime;
}
