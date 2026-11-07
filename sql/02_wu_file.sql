-- =============================================================================
-- wu-admin-cloud · wu_file 库
-- 表结构源自单体总库 admin_platform_prod.sql（按领域拆分），含各服务的自建表
-- 字符集: utf8mb4 / utf8mb4_unicode_ci
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `wu_file` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu_file`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE sys_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    dept_id BIGINT DEFAULT NULL COMMENT '归属部门ID',
    original_name VARCHAR(255) DEFAULT '' COMMENT '原始文件名',
    file_name VARCHAR(300) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    url VARCHAR(1024) DEFAULT '' COMMENT '访问URL',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(100) DEFAULT '' COMMENT 'MIME',
    file_suffix VARCHAR(32) DEFAULT '' COMMENT '后缀',
    storage_type VARCHAR(20) DEFAULT 'local' COMMENT '存储类型',
    bucket_name VARCHAR(100) DEFAULT '' COMMENT '桶名',
    group_id BIGINT DEFAULT NULL COMMENT '分组ID',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_group_id (group_id),
    INDEX idx_create_time (create_time),
    INDEX idx_group_time (group_id, create_time),
    INDEX idx_deleted_update (deleted, update_time),
    INDEX idx_file_path_bucket (file_path, bucket_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

DROP TABLE IF EXISTS `sys_file_group`;
CREATE TABLE sys_file_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    dept_id BIGINT DEFAULT NULL COMMENT '归属部门ID',
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    sort INT DEFAULT 0 COMMENT '排序',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分组表';

SET FOREIGN_KEY_CHECKS = 1;
