package com.wu.cloud.file.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FilePageDTO extends PageQuery {
    private String originalName;
    private Long groupId;
}
