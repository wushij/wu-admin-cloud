-- =============================================================================
-- wu-service-message 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_announce`;
CREATE TABLE IF NOT EXISTS sys_announce (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    notice_type TINYINT DEFAULT 1 COMMENT '1通知 2公告',
    channels VARCHAR(200) DEFAULT '["station"]' COMMENT '推送渠道JSON',
    target_type TINYINT DEFAULT 3 COMMENT '1指定用户 2按部门 3全部',
    target_ids VARCHAR(500) DEFAULT NULL COMMENT '目标ID JSON数组',
    status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_name VARCHAR(50) DEFAULT NULL COMMENT '创建人昵称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    updater VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    INDEX idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知/公告';

DROP TABLE IF EXISTS `sys_user_announce`;
CREATE TABLE IF NOT EXISTS sys_user_announce (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户',
    announce_id BIGINT NOT NULL COMMENT '通知ID',
    is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    read_time DATETIME DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_announce (user_id, announce_id),
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读';

DROP TABLE IF EXISTS `sys_announce_send_log`;
CREATE TABLE IF NOT EXISTS sys_announce_send_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announce_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL COMMENT 'station等',
    status TINYINT DEFAULT 1 COMMENT '0失败 1成功',
    target_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    error_msg VARCHAR(500) DEFAULT NULL,
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_announce_id (announce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知推送日志';

DROP TABLE IF EXISTS `sys_chat_message`;
CREATE TABLE IF NOT EXISTS sys_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(255) DEFAULT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT,
    msg_type TINYINT DEFAULT 1 COMMENT '1文本 2图片',
    is_read TINYINT DEFAULT 0,
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_receiver_unread (receiver_id, is_read, sender_id),
    INDEX idx_pair_time (sender_id, receiver_id, send_time),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

DROP TABLE IF EXISTS `sys_user_blacklist`;
CREATE TABLE IF NOT EXISTS sys_user_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '拉黑方',
    blocked_user_id BIGINT NOT NULL COMMENT '被拉黑用户',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天黑名单';

DROP TABLE IF EXISTS `sys_chat_group`;
CREATE TABLE IF NOT EXISTS sys_chat_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL,
    announcement VARCHAR(500) DEFAULT NULL,
    max_members INT DEFAULT 200,
    status TINYINT DEFAULT 1 COMMENT '0解散 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊';

DROP TABLE IF EXISTS `sys_chat_group_member`;
CREATE TABLE IF NOT EXISTS sys_chat_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    role TINYINT DEFAULT 0 COMMENT '0成员 1管理员 2群主',
    muted TINYINT DEFAULT 0,
    notify_muted TINYINT DEFAULT 0 COMMENT '0正常 1免打扰仅@提醒',
    announcement_read_time DATETIME DEFAULT NULL COMMENT '群公告已读时间',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员';

DROP TABLE IF EXISTS `sys_chat_group_message`;
CREATE TABLE IF NOT EXISTS sys_chat_group_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(50) DEFAULT NULL,
    sender_avatar VARCHAR(500) DEFAULT NULL,
    content TEXT NOT NULL,
    msg_type TINYINT DEFAULT 1,
    mention_ids VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表JSON',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, send_time),
    INDEX idx_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群消息';

DROP TABLE IF EXISTS `sys_chat_group_log`;
CREATE TABLE IF NOT EXISTS sys_chat_group_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群ID',
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    operator_id BIGINT NOT NULL COMMENT '操作人',
    operator_name VARCHAR(50) DEFAULT NULL COMMENT '操作人昵称',
    target_user_id BIGINT DEFAULT NULL COMMENT '目标用户',
    target_user_name VARCHAR(50) DEFAULT NULL COMMENT '目标用户昵称',
    detail VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group_time (group_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊操作日志';

SET FOREIGN_KEY_CHECKS = 1;
