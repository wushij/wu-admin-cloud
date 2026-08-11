package com.wu.cloud.codegen.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 待生成代码的表结构描述
 */
@Data
public class GenTable implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库表名，如 sys_notice */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 实体类名（不含后缀），如 SysNotice */
    private String className;

    /** 业务名（用于 URL 与包名），如 notice */
    private String businessName;

    /** 模块名（微服务名），如 system */
    private String moduleName;

    /** 功能名（用于注释），如 通知公告 */
    private String functionName;

    /** 生成包路径，如 com.wu.cloud.system */
    private String packageName;

    /** 作者 */
    private String author;

    /** 权限前缀，如 system:notice */
    private String permissionPrefix;

    /** 是否生成 Feign 契约与 Inner 控制器 */
    private boolean generateFeign;

    /** 字段列表 */
    private List<GenTableColumn> columns = new ArrayList<>();

    /** 主键字段 */
    public GenTableColumn getPkColumn() {
        return columns.stream().filter(GenTableColumn::isPk).findFirst().orElse(null);
    }
}
