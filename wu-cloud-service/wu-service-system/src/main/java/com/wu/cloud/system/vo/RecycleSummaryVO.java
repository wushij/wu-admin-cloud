package com.wu.cloud.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 回收站汇总统计 VO
 *
 * <p>字段与前端 {@code RecycleSummaryVO} 契约对齐。
 * 本服务仅能统计自身数据源（wu_system 库）内的表；
 * announce / ticket / file 等位于其它服务的库，由网关聚合或前端分别取数，
 * 此处对应字段保持为 0 并由调用方补齐。</p>
 */
@Data
public class RecycleSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userCount = 0L;

    private Long roleCount = 0L;

    private Long menuCount = 0L;

    private Long deptCount = 0L;

    private Long postCount = 0L;

    private Long dictTypeCount = 0L;

    private Long dictDataCount = 0L;

    /** 本服务内合计 */
    private Long totalCount = 0L;
}
