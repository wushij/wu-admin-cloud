-- =============================================================================
-- wu-admin-cloud · wu_infra 库
-- 表结构源自单体总库 admin_platform_prod.sql（按领域拆分），含各服务的自建表
-- 字符集: utf8mb4 / utf8mb4_unicode_ci
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `wu_infra` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu_infra`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE sys_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(64) DEFAULT 'DEFAULT' COMMENT '任务组名',
    invoke_target VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    cron_expression VARCHAR(255) DEFAULT NULL COMMENT 'cron执行表达式',
    misfire_policy TINYINT DEFAULT 3 COMMENT '计划执行错误策略(1-立即执行 2-执行一次 3-放弃执行)',
    concurrent TINYINT DEFAULT 1 COMMENT '是否并发执行(0-允许 1-禁止)',
    status TINYINT DEFAULT 0 COMMENT '状态(0-暂停 1-正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_job_group (job_group),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE sys_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    job_group VARCHAR(64) DEFAULT NULL COMMENT '任务组名',
    invoke_target VARCHAR(500) DEFAULT NULL COMMENT '调用目标字符串',
    job_message VARCHAR(500) DEFAULT NULL COMMENT '日志信息',
    status TINYINT DEFAULT 0 COMMENT '执行状态(0-正常 1-失败)',
    exception_info VARCHAR(2000) DEFAULT NULL COMMENT '异常信息',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    stop_time DATETIME DEFAULT NULL COMMENT '停止时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_name (job_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务日志表';

DROP TABLE IF EXISTS `sys_api_access_log`;
CREATE TABLE sys_api_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    start_time DATETIME NULL DEFAULT NULL COMMENT '请求开始时间',
    end_time DATETIME NULL DEFAULT NULL COMMENT '请求结束时间',
    api_path VARCHAR(500) NULL DEFAULT NULL COMMENT 'API路径',
    method VARCHAR(10) NULL DEFAULT NULL COMMENT 'HTTP方法',
    status_code INT NULL DEFAULT NULL COMMENT 'HTTP状态码',
    success TINYINT NULL DEFAULT 1 COMMENT '是否成功(0否 1是)',
    cost_time BIGINT NULL DEFAULT NULL COMMENT '耗时(毫秒)',
    ip VARCHAR(64) NULL DEFAULT NULL COMMENT '客户端IP',
    user_id BIGINT NULL DEFAULT NULL COMMENT '用户ID(未登录为空)',
    PRIMARY KEY (id),
    INDEX idx_start_time (start_time),
    INDEX idx_start_time_success (start_time, success),
    INDEX idx_api_path (api_path(100)),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问统计日志';

DROP TABLE IF EXISTS `sys_sms_log`;
CREATE TABLE sys_sms_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    content VARCHAR(500) DEFAULT NULL COMMENT '短信内容/验证码',
    sms_type VARCHAR(20) DEFAULT 'verify_code' COMMENT 'verify_code / notice / marketing',
    template_id VARCHAR(50) DEFAULT NULL COMMENT '模板ID',
    template_params VARCHAR(500) DEFAULT NULL COMMENT '模板参数 JSON',
    provider VARCHAR(20) DEFAULT NULL COMMENT 'aliyun / tencent / console',
    status TINYINT DEFAULT 0 COMMENT '0-发送中 1-成功 2-失败',
    result_msg VARCHAR(500) DEFAULT NULL COMMENT '结果信息',
    biz_id VARCHAR(100) DEFAULT NULL COMMENT '服务商消息ID',
    send_time DATETIME DEFAULT NULL COMMENT '发送时间',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    biz_type VARCHAR(30) DEFAULT NULL COMMENT '业务类型',
    ip VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone_create_time (phone, create_time),
    INDEX idx_create_time (create_time),
    INDEX idx_send_time (send_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信发送记录表';

DROP TABLE IF EXISTS `sys_email_log`;
CREATE TABLE `sys_email_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(200) NOT NULL,
  `subject` VARCHAR(500) DEFAULT NULL,
  `content` MEDIUMTEXT,
  `scene` VARCHAR(32) DEFAULT NULL,
  `provider` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT DEFAULT NULL,
  `result_msg` VARCHAR(500) DEFAULT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志';

DROP TABLE IF EXISTS `infra_access_log`;
CREATE TABLE `infra_access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) DEFAULT NULL,
  `user_id` VARCHAR(64) DEFAULT NULL,
  `user_name` VARCHAR(64) DEFAULT NULL,
  `application_name` VARCHAR(64) DEFAULT NULL,
  `request_url` VARCHAR(500) DEFAULT NULL,
  `request_method` VARCHAR(16) DEFAULT NULL,
  `user_agent` VARCHAR(500) DEFAULT NULL,
  `remote_addr` VARCHAR(64) DEFAULT NULL,
  `server_addr` VARCHAR(64) DEFAULT NULL,
  `status` INT DEFAULT NULL,
  `duration` BIGINT DEFAULT NULL,
  `access_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志';

DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE gen_table (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    table_name VARCHAR(200) DEFAULT '' COMMENT '表名称',
    `schema_name` VARCHAR(50) DEFAULT NULL COMMENT '所属微服务数据库',
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
CREATE TABLE gen_table_column (
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

-- ===== 初始数据 =====
INSERT INTO sys_job (id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, remark) VALUES
(1, '过期日志归档清理', 'SYSTEM', 'systemJobTask.purgeExpiredLogs', '0 30 2 * * ?', 3, 1, 0, '清理超保留期的操作/登录/API访问日志'),
(2, '私聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldChatMessages', '0 0 3 * * ?', 3, 1, 0, '清理超过 180 天的私聊记录'),
(3, '群聊消息清理', 'SYSTEM', 'systemJobTask.purgeOldGroupChatMessages', '0 10 3 * * ?', 3, 1, 0, '清理超过 180 天的群聊记录'),
(4, '调度日志清理', 'SYSTEM', 'systemJobTask.purgeExpiredJobLogs', '0 0 4 ? * SUN', 3, 1, 0, '清理 30 天前的 Quartz 调度执行日志'),
(5, '已读通知清理', 'SYSTEM', 'systemJobTask.purgeReadNotices', '0 15 3 * * ?', 3, 1, 0, '清理已读且超过 90 天的站内通知'),
(6, '工单回收站清理', 'SYSTEM', 'systemJobTask.purgeTicketRecycleBin', '0 30 3 * * ?', 3, 1, 0, '彻底删除回收站中超过 30 天的工单');

SET FOREIGN_KEY_CHECKS = 1;
