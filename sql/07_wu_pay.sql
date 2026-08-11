-- =============================================================================
-- wu-service-pay 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
  `pay_type` VARCHAR(32) NOT NULL COMMENT 'ALIPAY/WECHAT/BALANCE',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `transaction_id` VARCHAR(128) DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `paid_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';

DROP TABLE IF EXISTS `pay_refund`;
CREATE TABLE `pay_refund` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `refund_no` VARCHAR(64) NOT NULL,
  `order_no` VARCHAR(64) NOT NULL,
  `pay_type` VARCHAR(32) DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `reason` VARCHAR(500) DEFAULT NULL,
  `transaction_id` VARCHAR(128) DEFAULT NULL,
  `applicant_user_id` BIGINT DEFAULT NULL,
  `applicant_name` VARCHAR(64) DEFAULT NULL,
  `auditor_user_id` BIGINT DEFAULT NULL,
  `auditor_name` VARCHAR(64) DEFAULT NULL,
  `audit_remark` VARCHAR(500) DEFAULT NULL,
  `refund_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付退款';

DROP TABLE IF EXISTS `pay_config`;
CREATE TABLE `pay_config` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `pay_type` VARCHAR(32) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `app_id` VARCHAR(128) DEFAULT NULL,
  `mch_id` VARCHAR(128) DEFAULT NULL,
  `api_key` VARCHAR(512) DEFAULT NULL,
  `cert_path` VARCHAR(500) DEFAULT NULL,
  `cert_serial_no` VARCHAR(64) DEFAULT NULL,
  `notify_url` VARCHAR(500) DEFAULT NULL,
  `refund_notify_url` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_type` (`pay_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付配置';

SET FOREIGN_KEY_CHECKS = 1;
