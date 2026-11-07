-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: wu_system
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `wu_system`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `wu_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `wu_system`;

--
-- Table structure for table `sys_config_group`
--

DROP TABLE IF EXISTS `sys_config_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` varchar(50) NOT NULL COMMENT '分组编码 login/register',
  `group_name` varchar(100) NOT NULL COMMENT '分组名称',
  `config_value` text NOT NULL COMMENT 'JSON 配置',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator` varchar(64) DEFAULT '',
  `updater` varchar(64) DEFAULT '',
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置分组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config_group`
--

LOCK TABLES `sys_config_group` WRITE;
/*!40000 ALTER TABLE `sys_config_group` DISABLE KEYS */;
INSERT INTO `sys_config_group` VALUES (1,'site','基础信息','{\"platformName\":\"Admin Platform\",\"platformSubtitle\":\"统一运维 · 高效管控\",\"loginWelcome\":\"Welcome\",\"registerTitle\":\"Sign Up\",\"copyright\":\"\"}','平台展示名称与登录页文案','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (2,'session','会话配置','{\"tokenExpireHours\":24,\"sessionSignExpireHours\":24}','JWT 与 Redis 会话有效期（小时）','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (3,'file','文件配置','{\"type\":\"local\",\"localPath\":\"./data/uploads\",\"localDomain\":\"\",\"minioDomain\":\"\",\"minioEndpoint\":\"\",\"minioAccessKey\":\"\",\"minioSecretKey\":\"\",\"minioBucket\":\"wu-admin\",\"cosRegion\":\"\",\"cosBucket\":\"\",\"cosSecretId\":\"\",\"cosSecretKey\":\"\",\"cosDomain\":\"\",\"ossEndpoint\":\"\",\"ossBucket\":\"\",\"ossAccessKeyId\":\"\",\"ossAccessKeySecret\":\"\",\"ossDomain\":\"\",\"maxSizeMb\":50,\"allowedExtensions\":\"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov\"}','文件管理上传限制','2026-11-07 14:40:17','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_config_group` VALUES (4,'rateLimit','接口限流','{\"captchaPerIpMinute\":40,\"loginPerIpMinute\":30,\"registerPerIpMinute\":10,\"smsPerIpMinute\":5,\"smsSendIntervalSeconds\":60,\"smsPerPhoneDaily\":10,\"smsPerIpDaily\":30}','认证接口按 IP 限流；含短信防刷','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (5,'login','登录配置','{\"captchaEnabled\":true,\"captchaType\":\"image\",\"smsLoginEnabled\":false,\"smsLoginSliderCaptchaEnabled\":false,\"rememberMe\":true,\"maxRetryCount\":5,\"maxRetryCountIp\":20,\"lockTime\":10}','验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (6,'register','注册配置','{\"enabled\":true,\"captchaEnabled\":true,\"captchaType\":\"image\",\"defaultRoleCode\":\"user\",\"needAudit\":false,\"minPasswordLength\":6}','开放注册、验证码类型、默认角色、是否审核','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (7,'thirdParty','第三方配置','{\"wechat\":{\"enabled\":false,\"appId\":\"\",\"appSecret\":\"\"},\"alipay\":{\"enabled\":false,\"appId\":\"\",\"privateKey\":\"\",\"publicKey\":\"\"},\"github\":{\"enabled\":false,\"clientId\":\"\",\"clientSecret\":\"\"},\"google\":{\"enabled\":false,\"clientId\":\"\",\"clientSecret\":\"\",\"redirectUri\":\"\"}}','微信/支付宝/GitHub/Google 第三方登录密钥','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (8,'payment','支付配置','{\"wechatPay\":{\"enabled\":false,\"mchId\":\"\",\"appId\":\"\",\"apiV3Key\":\"\",\"privateKey\":\"\",\"certSerialNo\":\"\",\"notifyUrl\":\"\"},\"alipay\":{\"enabled\":false,\"appId\":\"\",\"privateKey\":\"\",\"publicKey\":\"\",\"signType\":\"RSA2\",\"gatewayUrl\":\"https://openapi.alipay.com/gateway.do\",\"notifyUrl\":\"\",\"returnUrl\":\"\"}}','微信/支付宝支付与测试下单','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (9,'sms','短信配置','{\"enabled\":false,\"provider\":\"aliyunAuth\",\"accessKeyId\":\"\",\"accessKeySecret\":\"\",\"signName\":\"\",\"tencentAppId\":\"\",\"templateVerifyCode\":\"100001\",\"templateModifyPhone\":\"100002\",\"templateResetPassword\":\"100003\",\"templateBindPhone\":\"100004\",\"templateVerifyBindPhone\":\"100005\",\"schemeName\":\"\",\"codeExpireMinutes\":5}','阿里云短信认证/腾讯云','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (10,'email','邮件配置','{\"enabled\":true,\"provider\":\"qq\",\"host\":\"smtp.qq.com\",\"port\":465,\"username\":\"974473458@qq.com\",\"password\":\"cqjvfpulydqwbegh\",\"fromName\":\"wu-admin 系统团队\",\"authEnabled\":true,\"securityType\":\"SSL\",\"connectionTimeoutMs\":5000,\"timeoutMs\":5000,\"writeTimeoutMs\":5000,\"encoding\":\"UTF-8\",\"debug\":false,\"codeExpireMinutes\":5,\"codeLength\":6,\"dailyLimitPerEmail\":20,\"sendIntervalSeconds\":60}','企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_config_group` VALUES (11,'security','安全配置','{\"disableDevtool\":false,\"isConcurrent\":false,\"sm2EncryptEnabled\":true,\"sm4EncryptEnabled\":true,\"sm2SignEnabled\":true,\"sm3SignEnabled\":true,\"timestampEnabled\":true,\"nonceEnabled\":true,\"riskEnabled\":true,\"riskWindowMinutes\":60,\"riskNoticeScore\":30,\"riskWarnScore\":50,\"riskChallengeScore\":70,\"riskBlockScore\":90,\"riskBanScore\":120,\"riskBlockSeconds\":3600,\"riskBanSeconds\":86400,\"riskChallengeEnabled\":true,\"riskBehaviorEnabled\":true,\"riskPageScanThreshold\":8,\"riskExportThreshold\":5,\"riskAlertWebhook\":\"\",\"honeypotEnabled\":true}','前端安全与会话：禁止调试、禁止多端同时在线','2026-11-07 14:40:17','2026-11-07 15:26:50','','',0);
INSERT INTO `sys_config_group` VALUES (12,'ai','AI助手配置','{\"globalKnowledge\":\"\",\"answerScope\":\"focus\",\"tokensPerUserDaily\":100000,\"roleTokenQuotas\":[]}','AI wu助手配置：answerScope 回答边界(focus/open)、Token 每日配额设置','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
/*!40000 ALTER TABLE `sys_config_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name` varchar(50) NOT NULL COMMENT '部门名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表，如 0,1,5',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '负责人',
  `leader_user_id` bigint DEFAULT NULL COMMENT '负责人用户ID',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
-- 组织架构：与单体 add11_standard_org_post.sql 对齐 —— 精简为 3 个标准部门。
-- 单体 full-install 脚本中「扁平部门树 → 中心分级」的旧迁移已被 add11 接管并注释掉，
-- 此处必须保持 3 节点标准结构，否则会把组织还原成 15 部门的旧树。
INSERT INTO `sys_dept` VALUES (1,'总公司',0,'0',1,1,'管理员',1,NULL,NULL,NOW(),NOW(),'','',0);
INSERT INTO `sys_dept` VALUES (2,'研发部',1,'0,1',1,1,'张三',2,NULL,NULL,NOW(),NOW(),'','',0);
INSERT INTO `sys_dept` VALUES (3,'运营部',1,'0,1',2,1,NULL,NULL,NULL,NULL,NOW(),NOW(),'','',0);
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型编码',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性',
  `list_class` varchar(100) DEFAULT NULL COMMENT '回显样式',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认(0否 1是)',
  `status` tinyint DEFAULT '1' COMMENT '状态(0停用 1正常)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` int DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_value` (`dict_type`,`dict_value`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_dict_type_status_deleted` (`dict_type`,`status`,`deleted`,`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,1,'启用','1','sys_normal_disable',NULL,'success',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (2,2,'禁用','0','sys_normal_disable',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (3,1,'男','1','sys_user_sex',NULL,'primary',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (4,2,'女','2','sys_user_sex',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (5,3,'未知','0','sys_user_sex',NULL,'info',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (6,1,'是','Y','sys_yes_no',NULL,'success',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (7,2,'否','N','sys_yes_no',NULL,'info',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (8,1,'待处理','OPEN','sys_ticket_status',NULL,'info',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (9,2,'处理中','IN_PROGRESS','sys_ticket_status',NULL,'warning',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (10,3,'已解决','RESOLVED','sys_ticket_status',NULL,'success',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (11,4,'已关闭','CLOSED','sys_ticket_status',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (12,1,'低','LOW','sys_ticket_priority',NULL,'info',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (13,2,'中','MEDIUM','sys_ticket_priority',NULL,'success',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (14,3,'高','HIGH','sys_ticket_priority',NULL,'warning',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (15,4,'紧急','URGENT','sys_ticket_priority',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (16,1,'通用','GENERAL','sys_approval_form_type',NULL,'info',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (17,2,'请假','LEAVE','sys_approval_form_type',NULL,'primary',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (18,3,'采购','PURCHASE','sys_approval_form_type',NULL,'warning',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (19,4,'报销','REIMBURSE','sys_approval_form_type',NULL,'success',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (20,5,'用印','SEAL','sys_approval_form_type',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (21,6,'合同','CONTRACT','sys_approval_form_type',NULL,'info',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (22,7,'注册审核','REGISTER','sys_approval_form_type',NULL,'warning',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (23,1,'待审批','SUBMITTED','sys_approval_status',NULL,'warning',1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (24,2,'已通过','APPROVED','sys_approval_status',NULL,'success',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (25,3,'已驳回','REJECTED','sys_approval_status',NULL,'danger',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_data` VALUES (26,4,'已归档','ARCHIVED','sys_approval_status',NULL,'info',0,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型编码',
  `status` tinyint DEFAULT '1' COMMENT '状态(0停用 1正常)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` int DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'系统状态','sys_normal_disable',1,'通用启用停用','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (2,'用户性别','sys_user_sex',1,'用户性别','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (3,'是否','sys_yes_no',1,'是或否','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (4,'工单状态','sys_ticket_status',1,'工单流转状态','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (5,'工单优先级','sys_ticket_priority',1,'工单优先级','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (6,'审批类型','sys_approval_form_type',1,'审批单业务类型','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_dict_type` VALUES (7,'审批状态','sys_approval_status',1,'审批单流转状态','2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_login_log`
--

DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `ipaddr` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `status` tinyint DEFAULT '0' COMMENT '登录状态(0-成功 1-失败)',
  `msg` varchar(255) DEFAULT NULL COMMENT '消息',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_login_log`
--

LOCK TABLES `sys_login_log` WRITE;
/*!40000 ALTER TABLE `sys_login_log` DISABLE KEYS */;
INSERT INTO `sys_login_log` VALUES (1,1,'admin','127.0.0.1','内网IP','Chrome','Windows 10/11',0,'登录成功','2026-11-07 17:38:09');
/*!40000 ALTER TABLE `sys_login_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `name` varchar(50) NOT NULL COMMENT '菜单名称',
  `permission` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `type` tinyint NOT NULL COMMENT '类型 1:目录 2:菜单 3:按钮',
  `sort` int DEFAULT '0' COMMENT '排序',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `path` varchar(200) DEFAULT NULL COMMENT '路由路径',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=236 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'系统管理','',1,1,0,'/system','Setting','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (2,'用户管理','system:user:list',2,1,1,'/system/user','User','system/user/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (3,'角色管理','system:role:list',2,2,1,'/system/role','UserFilled','system/role/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (4,'菜单管理','system:menu:list',2,3,1,'/system/menu','Menu','system/menu/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (5,'组织管理','system:dept:list',2,4,1,'/system/org','OfficeBuilding','system/org/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (6,'登录日志','system:loginLog:list',2,2,120,'/system/login-log','Promotion','system/login-log/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (7,'工单管理','system:ticket:list',2,2,8,'/system/ticket','Tickets','system/ticket/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (8,'流程中心','',1,7,0,'/workflow','Workflow','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (9,'审批单中心','system:approval:list',2,1,8,'/system/approval','Checked','system/approval/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (10,'用户查询','system:user:query',3,1,2,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (11,'用户新增','system:user:create',3,2,2,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (12,'用户修改','system:user:update',3,3,2,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (13,'用户删除','system:user:delete',3,4,2,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (20,'角色查询','system:role:query',3,1,3,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (21,'角色新增','system:role:create',3,2,3,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (22,'角色修改','system:role:update',3,3,3,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (23,'角色删除','system:role:delete',3,4,3,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (30,'菜单查询','system:menu:query',3,1,4,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (31,'菜单新增','system:menu:create',3,2,4,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (32,'菜单修改','system:menu:update',3,3,4,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (33,'菜单删除','system:menu:delete',3,4,4,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (40,'部门查询','system:dept:query',3,1,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (41,'部门新增','system:dept:create',3,2,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (42,'部门修改','system:dept:update',3,3,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (43,'部门删除','system:dept:delete',3,4,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (44,'岗位查询','system:post:query',3,5,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (45,'岗位新增','system:post:create',3,6,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (46,'岗位修改','system:post:update',3,7,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (47,'岗位删除','system:post:delete',3,8,5,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (50,'日志查询','system:loginLog:query',3,1,6,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (51,'日志删除','system:loginLog:delete',3,2,6,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (52,'日志清空','system:loginLog:clear',3,3,6,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (60,'工单查询','system:ticket:query',3,1,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (61,'工单创建','system:ticket:create',3,2,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (62,'工单更新','system:ticket:update',3,3,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (63,'工单流转','system:ticket:transition',3,4,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (64,'工单评论','system:ticket:comment',3,5,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (65,'工单删除','system:ticket:delete',3,6,7,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (70,'审批单查询','system:approval:query',3,1,9,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (71,'审批单创建','system:approval:create',3,2,9,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (72,'审批单审批','system:approval:approve',3,3,9,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (73,'审批单归档','system:approval:archive',3,4,9,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (74,'审批单删除','system:approval:delete',3,5,9,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (100,'系统监控','',1,3,0,'/monitor','Monitor','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (101,'API访问统计','monitor:apiAccess:list',2,6,100,'/monitor/api-access','DataLine','monitor/api-access/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (102,'访问统计查询','monitor:apiAccess:query',3,1,101,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (103,'在线用户','monitor:online:list',2,2,100,'/monitor/online','User','monitor/online/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (104,'在线用户强退','monitor:online:forceLogout',3,2,103,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (105,'文件管理','',1,5,0,'/file','Folder','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (110,'文件列表','sys:file:list',2,1,105,'/system/file','DocumentCopy','system/file/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (111,'文件查询','sys:file:query',3,1,110,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (112,'文件上传','sys:file:upload',3,2,110,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (113,'文件删除','sys:file:delete',3,3,110,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (120,'系统日志','',1,4,0,'/log','Calendar','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (121,'操作日志','system:operLog:list',2,1,120,'/system/oper-log','EditPen','system/oper-log/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (126,'操作日志查询','system:operLog:query',3,1,121,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (127,'操作日志删除','system:operLog:delete',3,2,121,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (128,'操作日志清空','system:operLog:clear',3,3,121,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (130,'字典管理','system:dict:list',2,5,1,'/system/dict','Collection','system/dict/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (131,'字典查询','system:dict:query',3,1,130,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (132,'字典新增','system:dict:create',3,2,130,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (133,'字典修改','system:dict:update',3,3,130,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (134,'字典删除','system:dict:delete',3,4,130,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (135,'字典复制','system:dict:copy',3,5,130,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (150,'开发工具','',1,9,0,'/tool','Tools','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (151,'接口文档','tool:apiDoc:view',2,1,150,'/tool/api-doc','Connection','/doc.html',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (160,'系统配置','system:config:list',2,7,1,'/system/config','Tools','system/config/index',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (161,'配置查询','system:config:query',3,1,160,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (162,'配置修改','system:config:update',3,2,160,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (163,'回收中心','system:recycle:list',2,6,1,'/system/recycle','Delete','system/recycle/index',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (164,'代码生成','tool:gen:list',2,2,150,'/tool/gen','SetUp','tool/gen/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (165,'代码生成查询','tool:gen:query',3,1,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (166,'代码生成导入','tool:gen:import',3,2,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (167,'代码生成修改','tool:gen:edit',3,3,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (168,'代码生成删除','tool:gen:remove',3,4,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (169,'代码生成预览','tool:gen:preview',3,5,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (170,'消息中心','',1,6,0,'/message','Bell','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (171,'系统通知','system:announce:list',2,1,170,'/message/notice','Notification','message/notice/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (172,'企业IM','system:chat:list',2,2,170,'/message/chat','ChatDotRound','message/chat/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (173,'通知查询','system:announce:query',3,1,171,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (174,'通知新增','system:announce:create',3,2,171,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (175,'通知修改','system:announce:update',3,3,171,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (176,'通知删除','system:announce:delete',3,4,171,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (177,'通知发布','system:announce:publish',3,5,171,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (178,'聊天查询','system:chat:query',3,1,172,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (179,'代码生成执行','tool:gen:code',3,6,164,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (180,'定时任务','monitor:job:list',2,3,100,'/monitor/job','Timer','monitor/job/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (181,'任务查询','monitor:job:query',3,1,180,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (182,'任务新增','monitor:job:add',3,2,180,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (183,'任务编辑','monitor:job:edit',3,3,180,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (184,'任务删除','monitor:job:delete',3,4,180,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (185,'缓存监控','monitor:cache:list',2,4,100,'/monitor/cache','Coin','monitor/cache/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (186,'缓存删除','monitor:cache:delete',3,2,185,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (187,'服务监控','monitor:server:list',2,5,100,'/monitor/server','Odometer','monitor/server/index',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (188,'缓存查询','monitor:cache:query',3,1,185,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (189,'服务监控查询','monitor:server:query',3,1,187,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (190,'在线用户查询','monitor:online:query',3,1,103,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (191,'回收中心查询','system:recycle:query',3,1,163,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (192,'回收中心恢复','system:recycle:restore',3,2,163,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (193,'回收中心删除','system:recycle:delete',3,3,163,'','','',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (200,'AI 模型配置','system:ai-model:list',2,1,210,'/ai/model','AiModel','ai/model/index',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (201,'AI模型新增','system:ai-model:create',3,2,200,'','','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (202,'AI模型修改','system:ai-model:update',3,3,200,'','','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (203,'AI模型删除','system:ai-model:delete',3,4,200,'','','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (204,'AI模型测试','system:ai-model:test',3,5,200,'','','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (205,'AI 对话日志','system:ai-log:list',2,2,210,'/ai/log','ChatDotRound','ai/log/index',1,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_menu` VALUES (206,'AI日志删除','system:ai-log:delete',3,2,205,'','','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (207,'AI模型查询','system:ai-model:query',3,1,200,'','','',1,'2026-09-05 14:16:48','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (208,'AI日志查询','system:ai-log:query',3,1,205,'','','',1,'2026-09-05 14:16:48','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (210,'AI 管理','',1,8,0,'/ai','Compass','',1,'2026-11-07 14:40:17','2026-11-07 17:47:37','','',0);
INSERT INTO `sys_menu` VALUES (220,'AI 知识库','system:ai-knowledge:list',2,3,210,'/ai/knowledge','Reading','ai/knowledge/index',1,'2026-08-28 16:59:46','2026-10-19 01:11:17','','',0);
INSERT INTO `sys_menu` VALUES (221,'AI知识新增','system:ai-knowledge:create',3,2,220,'','','',1,'2026-08-28 16:59:46','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (222,'AI知识修改','system:ai-knowledge:update',3,3,220,'','','',1,'2026-08-28 16:59:46','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (223,'AI知识删除','system:ai-knowledge:delete',3,4,220,'','','',1,'2026-08-28 16:59:46','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (224,'AI知识查询','system:ai-knowledge:query',3,1,220,'','','',1,'2026-09-05 14:16:48','2026-09-05 14:16:48','','',0);
INSERT INTO `sys_menu` VALUES (230,'安全风控','system:security:list',2,3,120,'/system/security-log','Lock','system/security-log/index',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_menu` VALUES (231,'安全查询','system:security:query',3,1,230,'','','',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_menu` VALUES (232,'风险解封','system:security:unblock',3,2,230,'','','',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_menu` VALUES (233,'手动拉黑','system:security:block',3,3,230,'','','',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_menu` VALUES (234,'清空安全日志','system:security:clear',3,4,230,'','','',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
INSERT INTO `sys_menu` VALUES (235,'删除安全日志','system:security:delete',3,5,230,'','','',1,'2026-11-07 15:11:51','2026-11-07 15:11:51','','',0);
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(50) DEFAULT NULL COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型(0其它 1新增 2修改 3删除 4查询 5导出 6导入)',
  `method` varchar(100) DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方式',
  `oper_name` varchar(50) DEFAULT NULL COMMENT '操作人员',
  `oper_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT NULL COMMENT '主机地址',
  `oper_param` varchar(2000) DEFAULT NULL COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT NULL COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态(0正常 1异常)',
  `error_msg` varchar(2000) DEFAULT NULL COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间(ms)',
  PRIMARY KEY (`id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_oper_name` (`oper_name`),
  KEY `idx_title` (`title`),
  KEY `idx_oper_time_status` (`oper_time`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
INSERT INTO `sys_oper_log` VALUES (1,'用户登录',5,'com.wu.cloud.auth.controller.AuthController.login','POST','admin','/api/auth/login','127.0.0.1','{\"loginType\":\"account\",\"username\":\"admin\",\"password\":\"******\"}',NULL,1,'请输入验证码','2026-11-07 16:52:28',24);
INSERT INTO `sys_oper_log` VALUES (2,'安全风控',2,'com.wu.cloud.system.controller.SecurityRiskController.unblock','POST','admin','/api/system/security/unblock','127.0.0.1','[\"ip:127.0.0.1\",true]','{\"code\":200,\"msg\":\"操作成功\",\"data\":true}',0,NULL,'2026-11-07 16:58:59',8);
INSERT INTO `sys_oper_log` VALUES (3,'用户登录',5,'com.wu.cloud.auth.controller.AuthController.login','POST','管理员','/api/auth/login','127.0.0.1','{\"loginType\":\"account\",\"username\":\"admin\",\"password\":\"******\",\"uuid\":\"1954ccbca9bb47f980e0105d35a78dc3\",\"captcha\":\"oseb\",\"code\":\"oseb\"}',NULL,0,NULL,'2026-11-07 17:38:09',1204);
INSERT INTO `sys_oper_log` VALUES (4,'系统用户',2,'com.wu.cloud.system.controller.SysUserController.updateUser','PUT','管理员','/api/system/user/update','127.0.0.1','{\"action\":\"修改用户「zhangsan」: 手机号码: 13800138001 -> ；岗位列表: 空 -> []\",\"diffItems\":[\"手机号码: 13800138001 -> \",\"岗位列表: 空 -> []\"],\"params\":{\"id\":2,\"username\":\"zhangsan\",\"nickname\":\"张三\",\"email\":\"zhangsan@admin.cn\",\"mobile\":\"\",\"sex\":0,\"status\":1,\"deptId\":6,\"roleId\":2,\"postIds\":[]}}','{\"code\":200,\"msg\":\"操作成功\",\"data\":true,\"traceId\":\"8d2489fbc68d494498a1da33573dc61f\"}',0,NULL,'2026-11-07 17:38:22',38);
INSERT INTO `sys_oper_log` VALUES (5,'系统用户',2,'com.wu.cloud.system.controller.SysUserController.updateUser','PUT','管理员','/api/system/user/update','127.0.0.1','{\"action\":\"修改用户「zhangsan」: 邮箱地址: zhangsan@admin.cn -> ；岗位列表: 空 -> []\",\"diffItems\":[\"邮箱地址: zhangsan@admin.cn -> \",\"岗位列表: 空 -> []\"],\"params\":{\"id\":2,\"username\":\"zhangsan\",\"nickname\":\"张三\",\"email\":\"\",\"mobile\":\"\",\"sex\":0,\"status\":1,\"deptId\":6,\"roleId\":2,\"postIds\":[]}}','{\"code\":200,\"msg\":\"操作成功\",\"data\":true,\"traceId\":\"3b53f824dfb44b62848da09eb93a8eb3\"}',0,NULL,'2026-11-07 17:38:26',12);
INSERT INTO `sys_oper_log` VALUES (6,'系统用户',2,'com.wu.cloud.system.controller.SysUserController.updateUser','PUT','管理员','/api/system/user/update','127.0.0.1','{\"action\":\"修改用户「admin」: 邮箱地址: admin@admin.cn -> ；手机号码: 13800138000 -> ；岗位列表: 空 -> []\",\"diffItems\":[\"邮箱地址: admin@admin.cn -> \",\"手机号码: 13800138000 -> \",\"岗位列表: 空 -> []\"],\"params\":{\"id\":1,\"username\":\"admin\",\"nickname\":\"管理员\",\"email\":\"\",\"mobile\":\"\",\"sex\":0,\"status\":1,\"deptId\":1,\"roleId\":1,\"postIds\":[]}}','{\"code\":200,\"msg\":\"操作成功\",\"data\":true,\"traceId\":\"638aa922bfa04dd1880532112182ac4b\"}',0,NULL,'2026-11-07 17:38:40',13);
INSERT INTO `sys_oper_log` VALUES (7,'系统配置 - 安全防刷',2,'com.wu.cloud.system.controller.SysConfigGroupController.updateConfigGroupByPath','PUT','管理员','/api/system/config-group/security','127.0.0.1','{\"action\":\"修改系统配置「安全防刷」: riskChallengeEnabled: 关闭 -> 开启；sm2EncryptEnabled: 无 -> 开启；riskBehaviorEnabled: 无 -> 开启；riskPageScanThreshold: 无 -> 8；riskExportThreshold: 无 -> 5；riskAlertWebhook: 无 -> ；honeypotEnabled: 无 -> 开启\",\"diffItems\":[\"riskChallengeEnabled: 关闭 -> 开启\",\"sm2EncryptEnabled: 无 -> 开启\",\"riskBehaviorEnabled: 无 -> 开启\",\"riskPageScanThreshold: 无 -> 8\",\"riskExportThreshold: 无 -> 5\",\"riskAlertWebhook: 无 -> \",\"honeypotEnabled: 无 -> 开启\"],\"params\":[\"security\",{\"configValue\":\"{\\\"disableDevtool\\\":false,\\\"isConcurrent\\\":false,\\\"sm2EncryptEnabled\\\":true,\\\"sm4EncryptEnabled\\\":true,\\\"sm2SignEnabled\\\":true,\\\"sm3SignEnabled\\\":true,\\\"timestampEnabled\\\":true,\\\"nonceEnabled\\\":true,\\\"riskEnabled\\\":true,\\\"riskWindowMinutes\\\":60,\\\"riskNoticeScore\\\":30,\\\"riskWarnScore\\\":50,\\\"riskChallengeScore\\\":70,\\\"riskBlockScore\\\":90,\\\"riskBanScore\\\":120,\\\"riskBlockSeconds\\\":3600,\\\"riskBanSeconds\\\":86400,\\\"riskChallengeEnabled\\\":true,\\\"riskBehaviorEnabled\\\":true,\\\"riskPageScanThreshold\\\":8,\\\"riskExportThreshold\\\":5,\\\"riskAlertWebhook\\\":\\\"\\\",\\\"honeypotEnabled\\\":true}\"}]}','{\"code\":200,\"msg\":\"操作成功\",\"data\":true,\"traceId\":\"f0cbf1b95eaa49bb92b37b407e15363b\"}',0,NULL,'2026-11-07 17:45:03',16);
INSERT INTO `sys_oper_log` VALUES (8,'AI模型配置',1,'com.wu.cloud.ai.controller.AiModelController.create','POST','管理员','/api/ai/model','127.0.0.1','{\"name\":\"deepseek\",\"provider\":\"deepseek\",\"protocol\":\"openai\",\"modelName\":\"deepseek-v4.1-flash\",\"baseUrl\":\"https://api.wushij.com/v1\",\"pathTemplate\":\"\",\"extraHeaders\":\"\",\"apiKey\":\"wbk_HnDQJzNr2JE4d_6Q9-R-GAcGU5sN1XWgNT7ZBzL49Yk\",\"temperature\":0.7,\"maxTokens\":8192,\"systemPrompt\":\"\",\"isDefault\":1,\"status\":1,\"remark\":\"\"}','{\"code\":200,\"msg\":\"保存模型配置成功\",\"data\":true,\"traceId\":\"52be255554824f5caa3f58bcccf31bfc\"}',0,NULL,'2026-11-07 17:46:14',232);
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父岗位ID',
  `post_code` varchar(50) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_code` (`post_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_post`
--

LOCK TABLES `sys_post` WRITE;
/*!40000 ALTER TABLE `sys_post` DISABLE KEYS */;
-- 岗位体系：与单体 add11_standard_org_post.sql 对齐 —— 精简为 3 个黄金岗位。
INSERT INTO `sys_post` VALUES (1,0,'ceo','总经理',1,1,'企业最高经营管理负责人',NOW(),NOW(),'','',0);
INSERT INTO `sys_post` VALUES (2,1,'cto','技术总监',2,1,'技术与研发整体负责人',NOW(),NOW(),'','',0);
INSERT INTO `sys_post` VALUES (3,2,'dev','研发工程师',3,1,'核心开发与技术实现',NOW(),NOW(),'','',0);
/*!40000 ALTER TABLE `sys_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `code` varchar(50) NOT NULL COMMENT '角色编码',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `data_scope` tinyint DEFAULT '1' COMMENT '数据范围 1:全部 2:自定义 3:本部门 4:本部门及以下 5:仅本人',
  `data_scope_dept_ids` varchar(500) DEFAULT NULL COMMENT '数据范围部门ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'超级管理员','super_admin',1,1,'超级管理员，拥有所有权限',1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_role` VALUES (2,'普通用户','user',2,1,'仅部分功能',4,NULL,'2026-11-07 14:40:17','2026-11-07 17:51:19','','',0);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=860 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1,1);
INSERT INTO `sys_role_menu` VALUES (2,1,2);
INSERT INTO `sys_role_menu` VALUES (3,1,3);
INSERT INTO `sys_role_menu` VALUES (4,1,4);
INSERT INTO `sys_role_menu` VALUES (5,1,5);
INSERT INTO `sys_role_menu` VALUES (15,1,6);
INSERT INTO `sys_role_menu` VALUES (12,1,7);
INSERT INTO `sys_role_menu` VALUES (13,1,8);
INSERT INTO `sys_role_menu` VALUES (14,1,9);
INSERT INTO `sys_role_menu` VALUES (21,1,10);
INSERT INTO `sys_role_menu` VALUES (22,1,11);
INSERT INTO `sys_role_menu` VALUES (23,1,12);
INSERT INTO `sys_role_menu` VALUES (24,1,13);
INSERT INTO `sys_role_menu` VALUES (25,1,20);
INSERT INTO `sys_role_menu` VALUES (26,1,21);
INSERT INTO `sys_role_menu` VALUES (27,1,22);
INSERT INTO `sys_role_menu` VALUES (28,1,23);
INSERT INTO `sys_role_menu` VALUES (29,1,30);
INSERT INTO `sys_role_menu` VALUES (30,1,31);
INSERT INTO `sys_role_menu` VALUES (31,1,32);
INSERT INTO `sys_role_menu` VALUES (32,1,33);
INSERT INTO `sys_role_menu` VALUES (33,1,40);
INSERT INTO `sys_role_menu` VALUES (34,1,41);
INSERT INTO `sys_role_menu` VALUES (35,1,42);
INSERT INTO `sys_role_menu` VALUES (36,1,43);
INSERT INTO `sys_role_menu` VALUES (37,1,44);
INSERT INTO `sys_role_menu` VALUES (38,1,45);
INSERT INTO `sys_role_menu` VALUES (39,1,46);
INSERT INTO `sys_role_menu` VALUES (40,1,47);
INSERT INTO `sys_role_menu` VALUES (48,1,50);
INSERT INTO `sys_role_menu` VALUES (49,1,51);
INSERT INTO `sys_role_menu` VALUES (50,1,52);
INSERT INTO `sys_role_menu` VALUES (51,1,60);
INSERT INTO `sys_role_menu` VALUES (52,1,61);
INSERT INTO `sys_role_menu` VALUES (53,1,62);
INSERT INTO `sys_role_menu` VALUES (54,1,63);
INSERT INTO `sys_role_menu` VALUES (55,1,64);
INSERT INTO `sys_role_menu` VALUES (56,1,65);
INSERT INTO `sys_role_menu` VALUES (57,1,70);
INSERT INTO `sys_role_menu` VALUES (58,1,71);
INSERT INTO `sys_role_menu` VALUES (59,1,72);
INSERT INTO `sys_role_menu` VALUES (60,1,73);
INSERT INTO `sys_role_menu` VALUES (61,1,74);
INSERT INTO `sys_role_menu` VALUES (62,1,100);
INSERT INTO `sys_role_menu` VALUES (63,1,101);
INSERT INTO `sys_role_menu` VALUES (64,1,102);
INSERT INTO `sys_role_menu` VALUES (65,1,103);
INSERT INTO `sys_role_menu` VALUES (67,1,104);
INSERT INTO `sys_role_menu` VALUES (78,1,105);
INSERT INTO `sys_role_menu` VALUES (79,1,110);
INSERT INTO `sys_role_menu` VALUES (80,1,111);
INSERT INTO `sys_role_menu` VALUES (81,1,112);
INSERT INTO `sys_role_menu` VALUES (82,1,113);
INSERT INTO `sys_role_menu` VALUES (16,1,120);
INSERT INTO `sys_role_menu` VALUES (17,1,121);
INSERT INTO `sys_role_menu` VALUES (18,1,126);
INSERT INTO `sys_role_menu` VALUES (19,1,127);
INSERT INTO `sys_role_menu` VALUES (20,1,128);
INSERT INTO `sys_role_menu` VALUES (6,1,130);
INSERT INTO `sys_role_menu` VALUES (41,1,131);
INSERT INTO `sys_role_menu` VALUES (42,1,132);
INSERT INTO `sys_role_menu` VALUES (43,1,133);
INSERT INTO `sys_role_menu` VALUES (44,1,134);
INSERT INTO `sys_role_menu` VALUES (45,1,135);
INSERT INTO `sys_role_menu` VALUES (83,1,150);
INSERT INTO `sys_role_menu` VALUES (84,1,151);
INSERT INTO `sys_role_menu` VALUES (7,1,160);
INSERT INTO `sys_role_menu` VALUES (46,1,161);
INSERT INTO `sys_role_menu` VALUES (47,1,162);
INSERT INTO `sys_role_menu` VALUES (8,1,163);
INSERT INTO `sys_role_menu` VALUES (85,1,164);
INSERT INTO `sys_role_menu` VALUES (86,1,165);
INSERT INTO `sys_role_menu` VALUES (87,1,166);
INSERT INTO `sys_role_menu` VALUES (88,1,167);
INSERT INTO `sys_role_menu` VALUES (89,1,168);
INSERT INTO `sys_role_menu` VALUES (90,1,169);
INSERT INTO `sys_role_menu` VALUES (92,1,170);
INSERT INTO `sys_role_menu` VALUES (93,1,171);
INSERT INTO `sys_role_menu` VALUES (94,1,172);
INSERT INTO `sys_role_menu` VALUES (95,1,173);
INSERT INTO `sys_role_menu` VALUES (96,1,174);
INSERT INTO `sys_role_menu` VALUES (97,1,175);
INSERT INTO `sys_role_menu` VALUES (98,1,176);
INSERT INTO `sys_role_menu` VALUES (99,1,177);
INSERT INTO `sys_role_menu` VALUES (100,1,178);
INSERT INTO `sys_role_menu` VALUES (91,1,179);
INSERT INTO `sys_role_menu` VALUES (68,1,180);
INSERT INTO `sys_role_menu` VALUES (69,1,181);
INSERT INTO `sys_role_menu` VALUES (70,1,182);
INSERT INTO `sys_role_menu` VALUES (71,1,183);
INSERT INTO `sys_role_menu` VALUES (72,1,184);
INSERT INTO `sys_role_menu` VALUES (73,1,185);
INSERT INTO `sys_role_menu` VALUES (74,1,186);
INSERT INTO `sys_role_menu` VALUES (75,1,187);
INSERT INTO `sys_role_menu` VALUES (76,1,188);
INSERT INTO `sys_role_menu` VALUES (77,1,189);
INSERT INTO `sys_role_menu` VALUES (66,1,190);
INSERT INTO `sys_role_menu` VALUES (9,1,191);
INSERT INTO `sys_role_menu` VALUES (10,1,192);
INSERT INTO `sys_role_menu` VALUES (11,1,193);
INSERT INTO `sys_role_menu` VALUES (102,1,200);
INSERT INTO `sys_role_menu` VALUES (103,1,201);
INSERT INTO `sys_role_menu` VALUES (104,1,202);
INSERT INTO `sys_role_menu` VALUES (105,1,203);
INSERT INTO `sys_role_menu` VALUES (106,1,204);
INSERT INTO `sys_role_menu` VALUES (107,1,205);
INSERT INTO `sys_role_menu` VALUES (108,1,206);
INSERT INTO `sys_role_menu` VALUES (515,1,207);
INSERT INTO `sys_role_menu` VALUES (516,1,208);
INSERT INTO `sys_role_menu` VALUES (101,1,210);
INSERT INTO `sys_role_menu` VALUES (234,1,220);
INSERT INTO `sys_role_menu` VALUES (235,1,221);
INSERT INTO `sys_role_menu` VALUES (236,1,222);
INSERT INTO `sys_role_menu` VALUES (237,1,223);
INSERT INTO `sys_role_menu` VALUES (517,1,224);
INSERT INTO `sys_role_menu` VALUES (632,1,230);
INSERT INTO `sys_role_menu` VALUES (633,1,231);
INSERT INTO `sys_role_menu` VALUES (634,1,232);
INSERT INTO `sys_role_menu` VALUES (635,1,233);
INSERT INTO `sys_role_menu` VALUES (636,1,234);
INSERT INTO `sys_role_menu` VALUES (637,1,235);
INSERT INTO `sys_role_menu` VALUES (799,2,1);
INSERT INTO `sys_role_menu` VALUES (801,2,2);
INSERT INTO `sys_role_menu` VALUES (803,2,3);
INSERT INTO `sys_role_menu` VALUES (804,2,4);
INSERT INTO `sys_role_menu` VALUES (805,2,5);
INSERT INTO `sys_role_menu` VALUES (806,2,6);
INSERT INTO `sys_role_menu` VALUES (807,2,7);
INSERT INTO `sys_role_menu` VALUES (808,2,8);
INSERT INTO `sys_role_menu` VALUES (809,2,9);
INSERT INTO `sys_role_menu` VALUES (810,2,10);
INSERT INTO `sys_role_menu` VALUES (811,2,20);
INSERT INTO `sys_role_menu` VALUES (814,2,30);
INSERT INTO `sys_role_menu` VALUES (818,2,40);
INSERT INTO `sys_role_menu` VALUES (821,2,44);
INSERT INTO `sys_role_menu` VALUES (824,2,50);
INSERT INTO `sys_role_menu` VALUES (829,2,60);
INSERT INTO `sys_role_menu` VALUES (831,2,61);
INSERT INTO `sys_role_menu` VALUES (833,2,62);
INSERT INTO `sys_role_menu` VALUES (835,2,64);
INSERT INTO `sys_role_menu` VALUES (836,2,70);
INSERT INTO `sys_role_menu` VALUES (837,2,71);
INSERT INTO `sys_role_menu` VALUES (838,2,72);
INSERT INTO `sys_role_menu` VALUES (840,2,73);
INSERT INTO `sys_role_menu` VALUES (851,2,100);
INSERT INTO `sys_role_menu` VALUES (852,2,101);
INSERT INTO `sys_role_menu` VALUES (853,2,102);
INSERT INTO `sys_role_menu` VALUES (854,2,103);
INSERT INTO `sys_role_menu` VALUES (855,2,105);
INSERT INTO `sys_role_menu` VALUES (856,2,110);
INSERT INTO `sys_role_menu` VALUES (857,2,111);
INSERT INTO `sys_role_menu` VALUES (858,2,112);
INSERT INTO `sys_role_menu` VALUES (859,2,120);
INSERT INTO `sys_role_menu` VALUES (800,2,130);
INSERT INTO `sys_role_menu` VALUES (802,2,131);
INSERT INTO `sys_role_menu` VALUES (812,2,150);
INSERT INTO `sys_role_menu` VALUES (813,2,151);
INSERT INTO `sys_role_menu` VALUES (815,2,160);
INSERT INTO `sys_role_menu` VALUES (816,2,161);
INSERT INTO `sys_role_menu` VALUES (817,2,163);
INSERT INTO `sys_role_menu` VALUES (819,2,170);
INSERT INTO `sys_role_menu` VALUES (820,2,171);
INSERT INTO `sys_role_menu` VALUES (822,2,172);
INSERT INTO `sys_role_menu` VALUES (823,2,173);
INSERT INTO `sys_role_menu` VALUES (825,2,178);
INSERT INTO `sys_role_menu` VALUES (826,2,180);
INSERT INTO `sys_role_menu` VALUES (827,2,181);
INSERT INTO `sys_role_menu` VALUES (828,2,187);
INSERT INTO `sys_role_menu` VALUES (830,2,189);
INSERT INTO `sys_role_menu` VALUES (832,2,190);
INSERT INTO `sys_role_menu` VALUES (834,2,191);
INSERT INTO `sys_role_menu` VALUES (839,2,200);
INSERT INTO `sys_role_menu` VALUES (841,2,205);
INSERT INTO `sys_role_menu` VALUES (842,2,206);
INSERT INTO `sys_role_menu` VALUES (843,2,207);
INSERT INTO `sys_role_menu` VALUES (844,2,208);
INSERT INTO `sys_role_menu` VALUES (845,2,210);
INSERT INTO `sys_role_menu` VALUES (846,2,220);
INSERT INTO `sys_role_menu` VALUES (847,2,221);
INSERT INTO `sys_role_menu` VALUES (848,2,222);
INSERT INTO `sys_role_menu` VALUES (849,2,223);
INSERT INTO `sys_role_menu` VALUES (850,2,224);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_security_event`
--

DROP TABLE IF EXISTS `sys_security_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `principal` varchar(128) NOT NULL DEFAULT '' COMMENT '主体标识(IP或账号)',
  `ipaddr` varchar(64) NOT NULL DEFAULT '' COMMENT '客户端真实IP',
  `request_uri` varchar(255) NOT NULL DEFAULT '' COMMENT '请求路径',
  `request_method` varchar(16) NOT NULL DEFAULT '' COMMENT '请求方法(GET/POST/PUT/DELETE)',
  `user_agent` varchar(512) NOT NULL DEFAULT '' COMMENT '请求User-Agent头',
  `risk_delta` int NOT NULL DEFAULT '0' COMMENT '本次触发风险分增量',
  `total_risk_score` int NOT NULL DEFAULT '0' COMMENT '触发后累计风险分',
  `action` varchar(32) NOT NULL DEFAULT 'BLOCKED' COMMENT '处置动作',
  `reason` varchar(255) NOT NULL DEFAULT '' COMMENT '触发原因详细说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_ipaddr` (`ipaddr`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_action_time` (`action`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全审计与反爬拦截事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_security_event`
--

LOCK TABLES `sys_security_event` WRITE;
/*!40000 ALTER TABLE `sys_security_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_security_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) NOT NULL COMMENT '昵称',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT '0' COMMENT '性别(0-未知 1-男 2-女)',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用 2:待审核 3:审核未通过',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_deleted_status` (`deleted`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','管理员','','',NULL,1,1,1,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
INSERT INTO `sys_user` VALUES (2,'zhangsan','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','张三','','',NULL,0,1,2,NULL,'2026-11-07 14:40:17','2026-11-07 14:40:17','','',0);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`,`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_post`
--

LOCK TABLES `sys_user_post` WRITE;
/*!40000 ALTER TABLE `sys_user_post` DISABLE KEYS */;
-- 用户岗位绑定：与单体 add11_standard_org_post.sql 对齐
INSERT INTO `sys_user_post` (user_id, post_id) VALUES
(1, 1),
(2, 3);
/*!40000 ALTER TABLE `sys_user_post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (5,1,1);
INSERT INTO `sys_user_role` VALUES (4,2,2);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-07 17:53:27
