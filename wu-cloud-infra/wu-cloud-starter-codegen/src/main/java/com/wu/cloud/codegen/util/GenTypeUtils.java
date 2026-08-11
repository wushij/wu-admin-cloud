package com.wu.cloud.codegen.util;

import java.util.Locale;
import java.util.Set;

/**
 * 代码生成类型与命名转换工具
 */
public final class GenTypeUtils {

    /** BaseEntity 已提供的公共字段，实体模板中需跳过 */
    public static final Set<String> BASE_COLUMNS = Set.of(
            "id", "create_time", "update_time", "creator", "updater", "deleted");

    /** 常见的表名前缀，生成类名时会被去除 */
    private static final Set<String> TABLE_PREFIXES = Set.of(
            "sys_", "biz_", "wu_", "t_", "tb_", "infra_");

    private GenTypeUtils() {
    }

    /**
     * 数据库列类型转 Java 类型
     *
     * @param columnType 数据库类型，可能带长度如 varchar(64)
     * @return Java 类型简单名
     */
    public static String toJavaType(String columnType) {
        if (columnType == null || columnType.isBlank()) {
            return "String";
        }
        // 去掉长度与 unsigned 等修饰，仅保留基础类型
        String type = columnType.toLowerCase(Locale.ROOT).trim();
        int idx = type.indexOf('(');
        if (idx > 0) {
            type = type.substring(0, idx);
        }
        type = type.split("\\s+")[0];

        return switch (type) {
            case "tinyint", "smallint", "mediumint", "int", "integer" -> "Integer";
            case "bigint" -> "Long";
            case "float" -> "Float";
            case "double" -> "Double";
            case "decimal", "numeric" -> "java.math.BigDecimal";
            case "bit", "boolean", "bool" -> "Boolean";
            case "date" -> "java.time.LocalDate";
            case "time" -> "java.time.LocalTime";
            case "datetime", "timestamp" -> "java.time.LocalDateTime";
            case "blob", "longblob", "mediumblob", "tinyblob", "binary", "varbinary" -> "byte[]";
            default -> "String";
        };
    }

    /**
     * 下划线转小驼峰：create_time -> createTime
     */
    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean upperNext = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '_') {
                upperNext = true;
                continue;
            }
            sb.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        return sb.toString();
    }

    /**
     * 下划线转大驼峰：sys_notice -> SysNotice
     */
    public static String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * 由表名推导类名，自动去除常见前缀：sys_notice -> Notice
     */
    public static String toClassName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return tableName;
        }
        String name = tableName.toLowerCase(Locale.ROOT);
        for (String prefix : TABLE_PREFIXES) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }
        return toPascalCase(name);
    }

    /**
     * 由表名推导业务名：sys_notice -> notice
     */
    public static String toBusinessName(String tableName) {
        String className = toClassName(tableName);
        if (className == null || className.isEmpty()) {
            return className;
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 判断是否为 BaseEntity 公共字段
     */
    public static boolean isBaseColumn(String columnName) {
        return columnName != null && BASE_COLUMNS.contains(columnName.toLowerCase(Locale.ROOT));
    }
}
