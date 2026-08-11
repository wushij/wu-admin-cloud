package com.wu.cloud.file.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件响应视图 VO
 */
@Data
public class FileRespVO implements Serializable {

    private Long id;
    private String originalName;
    private String fileName;
    private String url;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createTime;
}
