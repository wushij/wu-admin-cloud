package com.wu.cloud.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页统一响应对象
 *
 * @param <T> 数据模型类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }

    public static <T> PageResult<T> of(List<T> list, Long total) {
        return new PageResult<>(
                list == null ? Collections.emptyList() : list,
                total == null ? 0L : total);
    }
}
