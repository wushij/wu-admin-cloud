-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: wu_ai
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
-- Current Database: `wu_ai`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `wu_ai` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `wu_ai`;

--
-- Table structure for table `sys_ai_chat_log`
--

DROP TABLE IF EXISTS `sys_ai_chat_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_chat_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT '0' COMMENT '提问用户ID',
  `username` varchar(64) DEFAULT '' COMMENT '提问用户名',
  `conversation_id` varchar(64) DEFAULT '' COMMENT '会话ID（前端生成，串联多轮对话）',
  `model_id` bigint DEFAULT '0' COMMENT '模型配置ID',
  `provider` varchar(32) DEFAULT '' COMMENT '供应商',
  `model_name` varchar(100) DEFAULT '' COMMENT '模型名称',
  `question` text COMMENT '用户提问（脱敏后）',
  `answer` mediumtext COMMENT 'AI 回答',
  `prompt_tokens` int DEFAULT '0' COMMENT '提问 Token 消耗',
  `completion_tokens` int DEFAULT '0' COMMENT '回答 Token 消耗',
  `total_tokens` int DEFAULT '0' COMMENT '总 Token 消耗',
  `duration_ms` bigint DEFAULT '0' COMMENT '耗时（毫秒）',
  `chat_status` tinyint DEFAULT '1' COMMENT '结果 1:成功 0:失败 2:用户中断',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `source` varchar(20) DEFAULT 'pc' COMMENT '来源终端 pc/mobile',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  `tool_trace` text COMMENT '工具调用轨迹（工具名/是否拒绝/耗时）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话日志审计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_ai_chat_log`
--

LOCK TABLES `sys_ai_chat_log` WRITE;
/*!40000 ALTER TABLE `sys_ai_chat_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_ai_chat_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_ai_knowledge`
--

DROP TABLE IF EXISTS `sys_ai_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '知识ID',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '知识标题',
  `keywords` varchar(255) NOT NULL DEFAULT '' COMMENT '检索关键词(逗号分隔)',
  `content` text NOT NULL COMMENT '知识正文(Markdown)',
  `category` varchar(32) NOT NULL DEFAULT 'faq' COMMENT '分类: faq/manual/module/other',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重(大者优先)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 1启用 0停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_category` (`status`,`category`),
  FULLTEXT KEY `ft_knowledge` (`title`,`keywords`,`content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_ai_knowledge`
--

LOCK TABLES `sys_ai_knowledge` WRITE;
/*!40000 ALTER TABLE `sys_ai_knowledge` DISABLE KEYS */;
INSERT INTO `sys_ai_knowledge` VALUES (1,'忘记密码如何找回','忘记密码,找回密码,重置密码,登录不了','## 忘记密码找回流程\n1. 打开登录页，点击「忘记密码」。\n2. 输入账号绑定的邮箱，获取验证码。\n3. 输入验证码后设置新密码即可重新登录。\n\n> 若账号未绑定邮箱，请联系管理员在「系统管理 → 用户管理」中重置密码。','faq',10,1,'2026-08-28 16:59:02','2026-08-28 16:59:02','admin','admin',0);
INSERT INTO `sys_ai_knowledge` VALUES (2,'如何提交与跟进工单','工单,提交工单,报障,问题反馈','## 工单提交流程\n1. 进入「系统管理 → 工单管理」，点击「新建工单」。\n2. 填写标题、问题描述与优先级后提交。\n3. 处理进度可在工单列表查看，处理完成后可进行确认或评价。','manual',5,1,'2026-08-28 16:59:02','2026-08-28 16:59:02','admin','admin',0);
INSERT INTO `sys_ai_knowledge` VALUES (3,'系统核心功能模块与业务架构说明','系统模块,功能模块,系统功能,业务模块,有哪些模块,系统简介,系统介绍,平台功能','## 系统核心业务与功能模块概览\n本系统是一套企业级通用权限管理与运营中后台平台，包含以下核心模块：\n\n### 1. 权限与组织管理\n- **用户管理**：用户账号增删改查、部门分配、角色授权、密码重置。\n- **角色管理**：基于 RBAC 模型的角色管理与菜单/按钮级权限授权。\n- **部门与岗位管理**：多层级部门架构树维护及岗位职责关联。\n- **菜单管理**：动态路由配置、多端菜单与功能按钮权限管理。\n\n### 2. 业务流转与协作\n- **工单管理**：支持工单提交、工单派发、流转处理、进度跟进与完结评价。\n- **流程审批**：在线审批单申请、多级审批流转、催办与归档管理。\n- **公告与通知**：系统全局公告发布、站内消息实时推送与阅读状态跟踪。\n\n### 3. AI 智能副驾（AI wu助手）\n- **智能对话**：支持多轮对话与流式输出，解答系统使用疑问。\n- **业务工具联动（Function Calling）**：可实时查询待办数量、在线人数、登录统计与最新公告。\n- **知识库问答（RAG）**：支持企业 Markdown 文档全文检索与权威答疑。\n- **模型管理**：多大模型接入、提示词定制与调用审计日志。\n\n### 4. 基础设施与安全运维\n- **系统监控**：在线用户管控、服务器性能监控、日志审计（操作日志与登录日志）。\n- **系统配置与字典**：系统全局参数动态热更新、通用业务数据字典维护。\n- **代码生成**：支持单表/树表多端代码一键生成与下载。\n- **安全合规**：全链路国密传输加密、SM3/SM2 防篡改签名与严格权限控制。','manual',1,1,'2026-10-19 09:12:48','2026-10-27 13:14:18','admin','admin',1);
INSERT INTO `sys_ai_knowledge` VALUES (4,'wu-admin-system-guide','wu-admin-system-guide,wu-admin 企业级管理平台与 AI 智能中枢操作手册,一、系统简介与整体架构,1.1 技术架构栈,二、核心特色业务与安全防护规范,2.1 全链路国密数据加密机制,2.2 防机器人人机验证（滑块验证码）,2.3 多引擎对象存储系统','# wu-admin 企业级管理平台与 AI 智能中枢操作手册\n\n## 一、系统简介与整体架构\n\n**wu-admin** 是一套现代化企业级全栈管理后台系统，融合了深度安全防护机制与大语言模型（LLM）智能问答能力。\n\n### 1.1 技术架构栈\n- **后端架构**：采用 Spring Boot 3.5.13、Java 17、MyBatis-Plus 3.5.9、MySQL 8.0。\n- **安全体系**：全链路国密 SM4 对称加密、SM2 数字签名与防篡改、动态滑动拼图人机验证码、防暴力破解多级速率限制（RateLimit）。\n- **前端架构**：采用 Vue 3.4+、TypeScript 5.3+、Element Plus、Pinia 与 Vite。\n- **移动端架构**：基于 UniApp 跨端框架构建，适配 iOS、Android、微信小程序与 H5。\n- **AI 智能中枢**：集成 DeepSeek、通义千问、OpenAI、Kimi 等主流大模型，支持 SSE 流式打字机输出、多轮上下文记忆与本地 RAG 知识库优先检索。\n\n---\n\n## 二、核心特色业务与安全防护规范\n\n### 2.1 全链路国密数据加密机制\n系统全量敏感数据及传输网络接口均支持国密算法保护：\n- **数据传输加密**：采用满血标准 **SM4-GCM** 认证加密（AEAD：12 字节随机 IV + 密文 + 16 字节 GMAC Tag，接口请求头带 `X-Encrypted-Payload: SM4-GCM`），前端在请求前自动加密请求体并生成鉴真 Tag，后端过滤器完成解密与数据完整性校验；响应报文同理进行全密文安全传输，杜绝数据窃听与中间人篡改。\n- **签名防篡改**：采用 **SM2/HMAC-SM3** 对关键请求头（时间戳、随机数 nonce、流水号）进行数字签名与验签，防范重放攻击。\n- **敏感字段与文件存储**：敏感配置与大模型 API Key 入库前统一使用标准 **SM4-GCM**（带 `sm4-gcm:` 前缀）密文加密落库，杜绝内网敏感信息泄漏。\n\n### 2.2 防机器人人机验证（滑块验证码）\n- 登录、注册及找回密码等高敏场景接入了自主研发的**动态切块滑块人机验证**。\n- 后端在内存与 Redis 中比对滑动拼图的 X 轴偏移量（容差 <= 4px），结合滑动轨迹、加速度与时间窗口防范脚本自动化模拟刷接口。\n\n### 2.3 多引擎对象存储系统\n系统在“系统管理 -> 系统配置 -> 文件存储”中支持灵活热切换以下存储介质：\n1. **本地存储（Local）**：存储在服务器本地挂载磁盘，适合内网部署与单机测试。\n2. **MinIO 私有云存储**：适合私有化集群或 Docker 容器化编排。\n3. **阿里云 OSS**：支持企业级海量文件高可用存储与 CDN 加速。\n4. **腾讯云 COS**：适合腾讯云基础设施托管。\n\n---\n\n## 三、AI wu智能小助手与知识库联动机制\n\n系统右下角提供了浮动的 **AI wu小助手**（悬浮微光星芒图标），具备以下智能问答机制：\n\n### 3.1 本地知识库（RAG）优先检索原则\n1. 当用户向 AI 发起提问时，系统服务端会在调用大模型前，基于 **MySQL ngram 全文索引**与关键词模糊算法自动检索已启用的本地知识库文档。\n2. 若检索出匹配度高的知识文档，系统将自动将该文档作为最高优先级参考资料注入至提示词（Prompt）中。\n3. 大模型在回答时必须**严格以本地知识库文档内容为准**进行解答，严禁主观编造或虚构事实；只有知识库未收录的内容，方可结合通用知识进行指导。\n\n### 3.2 大模型供应商与参数配置\n在“AI 智能中心 -> AI 模型配置”中，管理员可维护多个大模型供应商：\n- 支持供应商：DeepSeek、OpenAI、通义千问、Kimi。\n- 核心配置项：接口地址（Base URL）、API Key 鉴权秘钥、温度系数（Temperature，推荐 0.7）、最大 Tokens（推荐 4096）、系统预设 Prompt。\n- 状态控制：支持一键连接可用性测试、启用/停用、以及“设为默认模型”。\n\n---\n\n## 四、常见问题（FAQ）与操作指南\n\n### Q1：忘记登录密码如何找回？\n**找回流程步骤**：\n1. 打开系统登录页，点击登录表单下方的「忘记密码」链接；\n2. 输入账号绑定的注册邮箱或手机号，点击获取 6 位数字验证码；\n3. 完成滑块拼图人机安全验证，输入收到的验证码；\n4. 设置并确认新密码（需包含大小写字母与数字，长度不低于 8 位）；\n5. 提交成功后即可使用新密码登录系统。\n\n### Q2：如何新增和配置一个大模型？\n**配置步骤**：\n1. 登录管理员账号，进入系统左侧菜单的「AI 智能中心 -> AI 模型配置」；\n2. 点击页面右上角的「新增模型」按钮，打开配置弹窗；\n3. 选择供应商（例如 DeepSeek），系统将自动回填官方接口地址及推荐模型名称；\n4. 粘贴您在对应平台申请的 API Key（秘钥会脱敏保存）；\n5. 点击底部的「测试」按钮，确保网络延迟与连通性正常（返回绿色毫秒延迟）；\n6. 保存后，在卡片右上角点击「设为默认」，即可生效为 AI 小助手的默认应答大模型。\n\n### Q3：如何将业务文档录入到 AI 知识库中？\n**录入方式**：\n- **方式一（批量上传）**：在「AI 知识库」页面点击「上传 MD 文档」按钮，支持将本地一个或多个 Markdown（.md）文档批量拖拽进弹窗，系统会自动解析文件名作为标题，并快速导入。\n- **方式二（在线新建）**：点击「新建文档」按钮，在分屏编辑器中直接编写 Markdown 标题与正文，支持实时预览，确认无误后保存即可。\n- **生效管理**：列表中每篇文档均有「检索生效」开关，仅开启生效状态的文档才会被 AI 助手检索并采用。\n\n### Q4：系统数据权限是如何控制与隔离的？\n**权限范围规则**：\n- 系统基于角色（Role）绑定部门数据范围：\n  1. **全部数据权限**：可查阅跨部门、跨层级所有业务数据。\n  2. **本部门及以下数据权限**：仅限查阅当前用户所在部门及下属子部门数据。\n  3. **本部门数据权限**：仅限查阅当前部门内的数据。\n  4. **仅本人数据权限**：系统自动在 SQL 层注入当前登录用户 ID 条件，仅查阅个人创建的数据。\n','manual',10,1,'2026-10-19 09:19:10','2026-10-19 09:19:10','admin','admin',0);
/*!40000 ALTER TABLE `sys_ai_knowledge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_ai_model`
--

DROP TABLE IF EXISTS `sys_ai_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型配置ID',
  `name` varchar(100) NOT NULL COMMENT '配置名称（展示用，如「DeepSeek 官方」）',
  `provider` varchar(32) NOT NULL COMMENT '供应商标识: deepseek/kimi/zhipu/openai/custom',
  `protocol` varchar(32) NOT NULL DEFAULT 'openai' COMMENT '接入协议: openai（OpenAI 兼容）/anthropic（Anthropic Messages）',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称（如 deepseek-v4.1-flash / glm-5.3 / claude-4.6）',
  `base_url` varchar(255) NOT NULL COMMENT 'API 基础地址（不含具体接口路径）',
  `path_template` varchar(500) DEFAULT '' COMMENT '自定义请求路径模板，支持 {model} 占位，空则按协议内置规则',
  `extra_headers` varchar(1000) DEFAULT '' COMMENT '额外请求头 JSON，如网关来源标识',
  `api_key` varchar(1024) DEFAULT '' COMMENT 'API 密钥（SM4-GCM 加密存储）',
  `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '采样温度 0~2',
  `max_tokens` int DEFAULT '8192' COMMENT '单次回复最大 Token 数',
  `system_prompt` varchar(2000) DEFAULT NULL COMMENT '系统提示词（角色设定）',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模型 0:否 1:是（全局唯一）',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `deleted` tinyint DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_provider` (`provider`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型供应商配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_ai_model`
--

LOCK TABLES `sys_ai_model` WRITE;
/*!40000 ALTER TABLE `sys_ai_model` DISABLE KEYS */;
INSERT INTO `sys_ai_model` VALUES (1,'deepseek','deepseek','openai','deepseek-v4.1-flash','https://api.wushij.com/v1','','','sm4-gcm:CkauwFZWfor4AhAu80EbFnwOg8vqKSVcJUIp7yBqeCi8hTST9I/SK1gRU2ONYpaqGceMLmqlI2faF0jxobrCksu9tP8yBwX/mLvL',0.70,8192,'',1,1,'','2026-11-07 17:46:14','2026-11-07 17:46:14','管理员','管理员',0);
/*!40000 ALTER TABLE `sys_ai_model` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-07 17:53:32
