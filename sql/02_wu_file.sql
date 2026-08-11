-- =============================================================================
-- wu-service-file 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    original_name VARCHAR(200) DEFAULT '' COMMENT '原始文件名',
    file_name VARCHAR(200) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    url VARCHAR(500) DEFAULT '' COMMENT '访问URL',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(100) DEFAULT '' COMMENT 'MIME',
    file_suffix VARCHAR(20) DEFAULT '' COMMENT '后缀',
    storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型',
    bucket_name VARCHAR(100) DEFAULT '' COMMENT '桶名',
    group_id BIGINT DEFAULT NULL COMMENT '分组ID',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time),
    INDEX idx_group_time (group_id, create_time),
    INDEX idx_deleted_update (deleted, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

DROP TABLE IF EXISTS sys_file_group;
CREATE TABLE sys_file_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    sort INT DEFAULT 0 COMMENT '排序',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分组表';

DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE IF NOT EXISTS gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    table_name VARCHAR(200) DEFAULT '' COMMENT '表名称',
    table_comment VARCHAR(500) DEFAULT '' COMMENT '表描述',
    class_name VARCHAR(100) DEFAULT '' COMMENT '实体类名称',
    package_name VARCHAR(100) DEFAULT '' COMMENT '生成包路径',
    module_name VARCHAR(30) DEFAULT '' COMMENT '生成模块名',
    business_name VARCHAR(30) DEFAULT '' COMMENT '生成业务名',
    function_name VARCHAR(50) DEFAULT '' COMMENT '生成功能名',
    author VARCHAR(50) DEFAULT '' COMMENT '作者',
    gen_type VARCHAR(10) DEFAULT 'crud' COMMENT '生成类型',
    gen_path VARCHAR(200) DEFAULT '/' COMMENT '生成路径',
    front_type VARCHAR(30) DEFAULT 'element-plus' COMMENT '前端模板',
    form_layout VARCHAR(20) DEFAULT 'vertical' COMMENT '表单布局',
    parent_menu_id BIGINT DEFAULT NULL COMMENT '上级菜单ID',
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_gen_table_name_deleted (table_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE IF NOT EXISTS gen_table_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_id BIGINT DEFAULT NULL,
    column_name VARCHAR(200) DEFAULT '',
    column_comment VARCHAR(500) DEFAULT '',
    column_type VARCHAR(100) DEFAULT '',
    java_type VARCHAR(50) DEFAULT '',
    java_field VARCHAR(200) DEFAULT '',
    is_pk TINYINT DEFAULT 0,
    is_increment TINYINT DEFAULT 0,
    is_required TINYINT DEFAULT 0,
    is_insert TINYINT DEFAULT 0,
    is_edit TINYINT DEFAULT 0,
    is_list TINYINT DEFAULT 0,
    is_query TINYINT DEFAULT 0,
    query_type VARCHAR(20) DEFAULT 'EQ',
    html_type VARCHAR(50) DEFAULT '',
    dict_type VARCHAR(100) DEFAULT '',
    sort INT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_gen_col_table (table_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段表';

SET FOREIGN_KEY_CHECKS = 1;
