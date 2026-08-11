package com.wu.cloud.common.core.domain;

import lombok.Data;
import java.io.Serializable;

/**
 * 分页查询基础请求参数
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 (默认 1) */
    private Integer pageNo = 1;

    /** 每页条数 (默认 10) */
    private Integer pageSize = 10;
}
