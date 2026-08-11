-- =============================================================================
-- wu-service-ticket 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_ticket;
CREATE TABLE sys_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    description TEXT COMMENT '工单描述',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级 LOW/MEDIUM/HIGH/URGENT',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN/IN_PROGRESS/RESOLVED/CLOSED',
    creator_user_id BIGINT NOT NULL COMMENT '创建人ID',
    assignee_user_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    deadline DATETIME DEFAULT NULL COMMENT '截止时间',
    closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_ticket_no (ticket_no),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_creator_user_id (creator_user_id),
    INDEX idx_assignee_user_id (assignee_user_id),
    INDEX idx_deleted_status_time (deleted, status, create_time),
    INDEX idx_deleted_update_time (deleted, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

DROP TABLE IF EXISTS sys_ticket_comment;
CREATE TABLE sys_ticket_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评论表';

DROP TABLE IF EXISTS sys_ticket_attachment;
CREATE TABLE sys_ticket_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    uploader_user_id BIGINT NOT NULL COMMENT '上传用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_uploader_user_id (uploader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件表';

DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
    biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
    read_status TINYINT DEFAULT 0 COMMENT '读取状态 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_read_deleted (user_id, read_status, deleted),
    INDEX idx_read_create_time (read_status, create_time),
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

DROP TABLE IF EXISTS sys_approval_form;
CREATE TABLE sys_approval_form (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    form_no VARCHAR(64) NOT NULL COMMENT '审批单号',
    form_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '类型',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态',
    applicant_user_id BIGINT NOT NULL COMMENT '申请人ID',
    approver_user_id BIGINT NOT NULL COMMENT '审批人ID',
    result_remark VARCHAR(1000) DEFAULT NULL COMMENT '审批结果备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_form_no (form_no),
    INDEX idx_status (status),
    INDEX idx_applicant_user_id (applicant_user_id),
    INDEX idx_approver_user_id (approver_user_id),
    INDEX idx_type_applicant_status (form_type, applicant_user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批单表';

-- 分库说明：单体脚本里「待审用户 → 审批单」种子依赖 sys_user，跨库不可用；运行时由业务写入。

DROP TABLE IF EXISTS sys_approval_record;
CREATE TABLE sys_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    form_id BIGINT NOT NULL COMMENT '审批单ID',
    operator_user_id BIGINT NOT NULL COMMENT '操作人ID',
    action VARCHAR(16) NOT NULL COMMENT '操作动作',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '操作备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_form_id (form_id),
    INDEX idx_operator_user_id (operator_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

DROP TABLE IF EXISTS `sys_approval_node`;
CREATE TABLE `sys_approval_node` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `form_id` BIGINT NOT NULL,
  `node_order` INT NOT NULL DEFAULT 1,
  `node_name` VARCHAR(100) NOT NULL,
  `approver_user_ids` VARCHAR(500) DEFAULT NULL,
  `strategy` VARCHAR(16) DEFAULT 'ANY',
  `status` VARCHAR(32) DEFAULT 'PENDING',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点';

DROP TABLE IF EXISTS `sys_approval_task`;
CREATE TABLE `sys_approval_task` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `node_id` BIGINT NOT NULL,
  `approver_user_id` BIGINT NOT NULL,
  `status` VARCHAR(32) DEFAULT 'PENDING',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

SET FOREIGN_KEY_CHECKS = 1;
