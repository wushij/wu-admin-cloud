-- =============================================================================
-- wu-service-order 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `subject` VARCHAR(200) DEFAULT NULL,
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `pay_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `pay_type` VARCHAR(32) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `pay_time` DATETIME DEFAULT NULL,
  `finish_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单';

DROP TABLE IF EXISTS `trade_order_item`;
CREATE TABLE `trade_order_item` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `order_id` BIGINT NOT NULL,
  `sku_code` VARCHAR(64) DEFAULT NULL,
  `sku_name` VARCHAR(200) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

DROP TABLE IF EXISTS `trade_refund`;
CREATE TABLE `trade_refund` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `refund_no` VARCHAR(64) NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `reason` VARCHAR(500) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `auditor_user_id` BIGINT DEFAULT NULL,
  `audit_remark` VARCHAR(500) DEFAULT NULL,
  `refund_time` DATETIME DEFAULT NULL,
  `transaction_id` VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易退款';

DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock` (

  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `updater` VARCHAR(64) DEFAULT '',
  `deleted` TINYINT NOT NULL DEFAULT 0,

  `sku_code` VARCHAR(64) NOT NULL,
  `sku_name` VARCHAR(200) NOT NULL,
  `total_stock` INT NOT NULL DEFAULT 0,
  `locked_stock` INT NOT NULL DEFAULT 0,
  `available_stock` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存';

SET FOREIGN_KEY_CHECKS = 1;
