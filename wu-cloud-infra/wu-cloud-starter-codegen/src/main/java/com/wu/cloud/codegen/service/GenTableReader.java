package com.wu.cloud.codegen.service;

import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.domain.GenTableColumn;
import com.wu.cloud.codegen.util.GenTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基于 information_schema 读取表结构元数据。
 * <p>
 * Database-per-Service 架构下，代码生成器寄居在 wu_infra 库，
 * 通过 {@code schemaName} 参数（白名单校验）跨库读取其他微服务库的表结构；
 * 未指定 schema 时读取当前库。
 * </p>
 */
@Slf4j
public class GenTableReader {

    /** 允许读取的微服务数据库白名单，防止任意 schema 探测 */
    public static final Set<String> ALLOWED_SCHEMAS = Set.of(
            "wu_system", "wu_file", "wu_ticket", "wu_ai",
            "wu_message", "wu_infra", "wu_pay", "wu_order");

    private final JdbcTemplate jdbcTemplate;

    public GenTableReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询指定库中的所有业务表（排除代码生成器自身的表）
     *
     * @param tableNameLike 表名模糊匹配，可为空
     * @param schemaName    所属微服务数据库，可为空（默认当前库）
     */
    public List<Map<String, Object>> listTables(String tableNameLike, String schemaName) {
        boolean useSchema = isValidSchema(schemaName);
        String schemaCondition = useSchema ? "'" + schemaName + "'" : "(SELECT DATABASE())";
        String sql = "SELECT table_name AS tableName, table_comment AS tableComment, "
                + "create_time AS createTime FROM information_schema.tables "
                + "WHERE table_schema = " + schemaCondition + " AND table_type = 'BASE TABLE' "
                + "AND table_name NOT IN ('gen_table', 'gen_table_column') ";
        if (tableNameLike != null && !tableNameLike.isBlank()) {
            sql += "AND table_name LIKE ? ORDER BY create_time DESC";
            return jdbcTemplate.queryForList(sql, "%" + tableNameLike.trim() + "%");
        }
        sql += "ORDER BY create_time DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 读取指定表的完整结构并填充默认生成配置
     *
     * @param tableName   表名
     * @param schemaName  所属微服务数据库，可为空（默认当前库）
     * @param moduleName  所属微服务模块名，如 system
     * @param packageName 生成包名，如 com.wu.cloud.system
     * @param author      作者
     */
    public GenTable readTable(String tableName, String schemaName, String moduleName, String packageName, String author) {
        GenTable table = new GenTable();
        table.setTableName(tableName);
        table.setModuleName(moduleName);
        table.setPackageName(packageName);
        table.setAuthor(author);

        // 表注释
        List<Map<String, Object>> tableInfo = jdbcTemplate.queryForList(
                "SELECT table_comment AS tableComment FROM information_schema.tables "
                        + "WHERE table_schema = " + schemaExpr(schemaName) + " AND table_name = ?", tableName);
        String comment = tableInfo.isEmpty() ? "" : str(tableInfo.get(0).get("tableComment"));
        table.setTableComment(comment);

        String className = GenTypeUtils.toClassName(tableName);
        table.setClassName(className);
        table.setBusinessName(GenTypeUtils.toBusinessName(tableName));
        // 去掉注释里常见的“表”后缀，作为功能名
        table.setFunctionName(comment == null || comment.isBlank()
                ? className : comment.replaceAll("表$", "").trim());
        table.setPermissionPrefix(moduleName + ":" + table.getBusinessName());
        table.setGenerateFeign(false);

        table.setColumns(readColumns(tableName, schemaName));
        return table;
    }

    /**
     * 读取字段列表
     */
    private List<GenTableColumn> readColumns(String tableName, String schemaName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT column_name AS columnName, column_comment AS columnComment, "
                        + "column_type AS columnType, is_nullable AS isNullable, "
                        + "column_key AS columnKey, extra AS extra "
                        + "FROM information_schema.columns "
                        + "WHERE table_schema = " + schemaExpr(schemaName) + " AND table_name = ? "
                        + "ORDER BY ordinal_position", tableName);

        List<GenTableColumn> columns = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            GenTableColumn column = new GenTableColumn();
            String columnName = str(row.get("columnName"));
            column.setColumnName(columnName);
            column.setColumnComment(str(row.get("columnComment")));
            column.setColumnType(str(row.get("columnType")));
            column.setJavaField(GenTypeUtils.toCamelCase(columnName));
            column.setJavaType(GenTypeUtils.toJavaType(str(row.get("columnType"))));

            String columnKey = str(row.get("columnKey"));
            String extra = str(row.get("extra"));
            column.setPk("PRI".equalsIgnoreCase(columnKey));
            column.setIncrement(extra != null && extra.toLowerCase(Locale.ROOT).contains("auto_increment"));
            column.setRequired("NO".equalsIgnoreCase(str(row.get("isNullable"))) && !column.isPk());
            column.setBaseField(GenTypeUtils.isBaseColumn(columnName));

            // 公共字段与主键默认不参与表单与查询
            boolean business = !column.isBaseField();
            column.setForm(business);
            column.setList(business);
            column.setQuery(business && isQueryCandidate(column));
            column.setQueryType("String".equals(column.getJavaType()) ? "LIKE" : "EQ");
            columns.add(column);
        }
        return columns;
    }

    /**
     * 跨白名单库查找表所属 schema（用于历史数据或未显式指定 schema 的场景）
     *
     * @param tableName 表名
     * @return 命中库名；未命中返回 null（交由当前库兜底）
     */
    public String findSchemaOf(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return null;
        }
        for (String schema : ALLOWED_SCHEMAS) {
            try {
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE'",
                        Long.class, schema, tableName);
                if (count != null && count > 0) {
                    return schema;
                }
            } catch (Exception e) {
                log.warn("[Codegen] 探测表 {} 所属库 {} 失败: {}", tableName, schema, e.getMessage());
            }
        }
        return null;
    }

    private boolean isValidSchema(String schemaName) {
        return schemaName != null && !schemaName.isBlank() && ALLOWED_SCHEMAS.contains(schemaName);
    }

    private String schemaExpr(String schemaName) {
        return isValidSchema(schemaName) ? "'" + schemaName + "'" : "(SELECT DATABASE())";
    }

    /**
     * 名称、编码、状态、类型类字段默认作为查询条件
     */
    private boolean isQueryCandidate(GenTableColumn column) {
        String field = column.getJavaField() == null ? "" : column.getJavaField().toLowerCase(Locale.ROOT);
        return field.contains("name") || field.contains("code") || field.contains("status")
                || field.contains("type") || field.contains("title");
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
