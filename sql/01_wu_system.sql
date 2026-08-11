-- =============================================================================
-- wu-service-system 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    gender TINYINT DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用 2:待审核 3:审核未通过',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_username (username),
    INDEX idx_mobile (mobile),
    INDEX idx_dept_id (dept_id),
    INDEX idx_deleted_status (deleted, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT INTO sys_user (id, username, password, nickname, mobile, email, status, dept_id) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', '13800138000', 'admin@admin.cn', 1, 1),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '13800138001', 'zhangsan@admin.cn', 1, 6);

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL COMMENT '角色编码',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    data_scope TINYINT DEFAULT 1 COMMENT '数据范围 1:全部 2:自定义 3:本部门 4:本部门及以下 5:仅本人',
    data_scope_dept_ids VARCHAR(500) DEFAULT NULL COMMENT '数据范围部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

INSERT INTO sys_role (id, name, code, sort, status, remark) VALUES
(1, '超级管理员', 'super_admin', 1, 1, '超级管理员，拥有所有权限'),
(2, '普通用户', 'user', 2, 1, '仅部分功能');

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    permission VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    type TINYINT NOT NULL COMMENT '类型 1:目录 2:菜单 3:按钮',
    sort INT DEFAULT 0 COMMENT '排序',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    path VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    icon VARCHAR(100) DEFAULT NULL COMMENT '图标',
    component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
-- 系统管理目录
(1, '系统管理', '', 1, 1, 0, '/system', 'Setting', '', 1),
-- 用户管理
(2, '用户管理', 'system:user:list', 2, 1, 1, '/system/user', 'User', 'system/user/index', 1),
-- 角色管理
(3, '角色管理', 'system:role:list', 2, 2, 1, '/system/role', 'UserFilled', 'system/role/index', 1),
-- 菜单管理
(4, '菜单管理', 'system:menu:list', 2, 3, 1, '/system/menu', 'Menu', 'system/menu/index', 1),
-- 组织管理（部门 + 岗位）
(5, '组织管理', 'system:dept:list', 2, 4, 1, '/system/org', 'OfficeBuilding', 'system/org/index', 1),
-- 字典管理
(130, '字典管理', 'system:dict:list', 2, 5, 1, '/system/dict', 'Collection', 'system/dict/index', 1),
(160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
(163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1),
(191, '回收中心查询', 'system:recycle:query', 3, 1, 163, '', '', '', 1),
(192, '回收中心恢复', 'system:recycle:restore', 3, 2, 163, '', '', '', 1),
(193, '回收中心删除', 'system:recycle:delete', 3, 3, 163, '', '', '', 1),
-- 流程中心目录（审批 + 工单）
(8, '流程中心', '', 1, 7, 0, '/workflow', 'Operation', '', 1),
-- 审批单中心
(9, '审批单中心', 'system:approval:list', 2, 1, 8, '/system/approval', 'Checked', 'system/approval/index', 1),
-- 工单管理（隶属流程中心）
(7, '工单管理', 'system:ticket:list', 2, 2, 8, '/system/ticket', 'Tickets', 'system/ticket/index', 1),
-- 用户管理按钮
(10, '用户查询', 'system:user:query', 3, 1, 2, '', '', '', 1),
(11, '用户新增', 'system:user:create', 3, 2, 2, '', '', '', 1),
(12, '用户修改', 'system:user:update', 3, 3, 2, '', '', '', 1),
(13, '用户删除', 'system:user:delete', 3, 4, 2, '', '', '', 1),
-- 角色管理按钮
(20, '角色查询', 'system:role:query', 3, 1, 3, '', '', '', 1),
(21, '角色新增', 'system:role:create', 3, 2, 3, '', '', '', 1),
(22, '角色修改', 'system:role:update', 3, 3, 3, '', '', '', 1),
(23, '角色删除', 'system:role:delete', 3, 4, 3, '', '', '', 1),
-- 菜单管理按钮
(30, '菜单查询', 'system:menu:query', 3, 1, 4, '', '', '', 1),
(31, '菜单新增', 'system:menu:create', 3, 2, 4, '', '', '', 1),
(32, '菜单修改', 'system:menu:update', 3, 3, 4, '', '', '', 1),
(33, '菜单删除', 'system:menu:delete', 3, 4, 4, '', '', '', 1),
-- 部门管理按钮
(40, '部门查询', 'system:dept:query', 3, 1, 5, '', '', '', 1),
(41, '部门新增', 'system:dept:create', 3, 2, 5, '', '', '', 1),
(42, '部门修改', 'system:dept:update', 3, 3, 5, '', '', '', 1),
(43, '部门删除', 'system:dept:delete', 3, 4, 5, '', '', '', 1),
-- 岗位管理按钮（组织管理页内）
(44, '岗位查询', 'system:post:query', 3, 5, 5, '', '', '', 1),
(45, '岗位新增', 'system:post:create', 3, 6, 5, '', '', '', 1),
(46, '岗位修改', 'system:post:update', 3, 7, 5, '', '', '', 1),
(47, '岗位删除', 'system:post:delete', 3, 8, 5, '', '', '', 1),
-- 字典管理按钮
(131, '字典查询', 'system:dict:query', 3, 1, 130, '', '', '', 1),
(132, '字典新增', 'system:dict:create', 3, 2, 130, '', '', '', 1),
(133, '字典修改', 'system:dict:update', 3, 3, 130, '', '', '', 1),
(134, '字典删除', 'system:dict:delete', 3, 4, 130, '', '', '', 1),
(135, '字典复制', 'system:dict:copy', 3, 5, 130, '', '', '', 1),
(161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
(162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1),
-- 登录日志按钮
(50, '日志查询', 'system:loginLog:query', 3, 1, 6, '', '', '', 1),
(51, '日志删除', 'system:loginLog:delete', 3, 2, 6, '', '', '', 1),
(52, '日志清空', 'system:loginLog:clear', 3, 3, 6, '', '', '', 1),
-- 工单按钮
(60, '工单查询', 'system:ticket:query', 3, 1, 7, '', '', '', 1),
(61, '工单创建', 'system:ticket:create', 3, 2, 7, '', '', '', 1),
(62, '工单更新', 'system:ticket:update', 3, 3, 7, '', '', '', 1),
(63, '工单流转', 'system:ticket:transition', 3, 4, 7, '', '', '', 1),
(64, '工单评论', 'system:ticket:comment', 3, 5, 7, '', '', '', 1),
(65, '工单删除', 'system:ticket:delete', 3, 6, 7, '', '', '', 1),
-- 审批单按钮
(70, '审批单查询', 'system:approval:query', 3, 1, 9, '', '', '', 1),
(71, '审批单创建', 'system:approval:create', 3, 2, 9, '', '', '', 1),
(72, '审批单审批', 'system:approval:approve', 3, 3, 9, '', '', '', 1),
(73, '审批单归档', 'system:approval:archive', 3, 4, 9, '', '', '', 1),
(74, '审批单删除', 'system:approval:delete', 3, 5, 9, '', '', '', 1),
-- 文件管理目录（与系统管理、系统监控同级）
(105, '文件管理', '', 1, 5, 0, '/file', 'Folder', '', 1),
(110, '文件列表', 'sys:file:list', 2, 1, 105, '/system/file', 'DocumentCopy', 'system/file/index', 1),
(111, '文件查询', 'sys:file:query', 3, 1, 110, '', '', '', 1),
(112, '文件上传', 'sys:file:upload', 3, 2, 110, '', '', '', 1),
(113, '文件删除', 'sys:file:delete', 3, 3, 110, '', '', '', 1),
-- 系统监控目录
(100, '系统监控', '', 1, 3, 0, '/monitor', 'Monitor', '', 1),
-- API 访问统计
(101, 'API访问统计', 'monitor:apiAccess:list', 2, 6, 100, '/monitor/api-access', 'DataLine', 'monitor/api-access/index', 1),
(102, '访问统计查询', 'monitor:apiAccess:query', 3, 1, 101, '', '', '', 1),
-- 在线用户
(103, '在线用户', 'monitor:online:list', 2, 2, 100, '/monitor/online', 'User', 'monitor/online/index', 1),
(190, '在线用户查询', 'monitor:online:query', 3, 1, 103, '', '', '', 1),
(104, '在线用户强退', 'monitor:online:forceLogout', 3, 2, 103, '', '', '', 1),
-- 定时任务
(180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
(181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
(182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
(183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
(184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1),
-- 缓存监控
(185, '缓存监控', 'monitor:cache:list', 2, 4, 100, '/monitor/cache', 'Coin', 'monitor/cache/index', 1),
(188, '缓存查询', 'monitor:cache:query', 3, 1, 185, '', '', '', 1),
(186, '缓存删除', 'monitor:cache:delete', 3, 2, 185, '', '', '', 1),
-- 服务监控
(187, '服务监控', 'monitor:server:list', 2, 5, 100, '/monitor/server', 'Cpu', 'monitor/server/index', 1),
(189, '服务监控查询', 'monitor:server:query', 3, 1, 187, '', '', '', 1),
-- 系统日志目录
(120, '系统日志', '', 1, 4, 0, '/log', 'Notebook', '', 1),
(121, '操作日志', 'system:operLog:list', 2, 1, 120, '/system/oper-log', 'EditPen', 'system/oper-log/index', 1),
(126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1),
(127, '操作日志删除', 'system:operLog:delete', 3, 2, 121, '', '', '', 1),
(128, '操作日志清空', 'system:operLog:clear', 3, 3, 121, '', '', '', 1),
-- 登录日志（隶属系统日志）
(6, '登录日志', 'system:loginLog:list', 2, 2, 120, '/system/login-log', 'Promotion', 'system/login-log/index', 1),
-- 开发工具
(150, '开发工具', '', 1, 9, 0, '/tool', 'Tools', '', 1),
(151, '接口文档', 'tool:apiDoc:view', 2, 1, 150, '/tool/api-doc', 'Connection', '/doc.html', 1),
(164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'SetUp', 'tool/gen/index', 1),
(165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
(166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
(167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
(168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
(169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
(179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1),
-- 消息中心
(170, '消息中心', '', 1, 6, 0, '/message', 'Bell', '', 1),
(171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
(172, '企业IM', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
(173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
(174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
(175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
(176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
(177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
(178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1),
-- AI 管理（顶级目录 210，排在开发工具之前）
(210, 'AI 管理', '', 1, 8, 0, '/ai', 'MagicStick', '', 1),
(200, 'AI 模型配置', 'system:ai-model:list', 2, 1, 210, '/ai/model', 'MagicStick', 'ai/model/index', 1),
(201, 'AI模型新增', 'system:ai-model:create', 3, 1, 200, '', '', '', 1),
(202, 'AI模型修改', 'system:ai-model:update', 3, 2, 200, '', '', '', 1),
(203, 'AI模型删除', 'system:ai-model:delete', 3, 3, 200, '', '', '', 1),
(204, 'AI模型测试', 'system:ai-model:test', 3, 4, 200, '', '', '', 1),
(205, 'AI 对话日志', 'system:ai-log:list', 2, 2, 210, '/ai/log', 'ChatDotRound', 'ai/log/index', 1),
(206, 'AI日志删除', 'system:ai-log:delete', 3, 1, 205, '', '', '', 1);

INSERT INTO sys_menu (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
        (160, '系统配置', 'system:config:list', 2, 6, 1, '/system/config', 'Tools', 'system/config/index', 1),
        (163, '回收中心', 'system:recycle:list', 2, 7, 1, '/system/recycle', 'Delete', 'system/recycle/index', 1),
        (161, '配置查询', 'system:config:query', 3, 1, 160, '', '', '', 1),
        (162, '配置修改', 'system:config:update', 3, 2, 160, '', '', '', 1),
        (126, '操作日志查询', 'system:operLog:query', 3, 1, 121, '', '', '', 1),
        (170, '消息中心', '', 1, 6, 0, '/message', 'Bell', '', 1),
        (171, '系统通知', 'system:announce:list', 2, 1, 170, '/message/notice', 'Notification', 'message/notice/index', 1),
        (172, '企业IM', 'system:chat:list', 2, 2, 170, '/message/chat', 'ChatDotRound', 'message/chat/index', 1),
        (173, '通知查询', 'system:announce:query', 3, 1, 171, '', '', '', 1),
        (174, '通知新增', 'system:announce:create', 3, 2, 171, '', '', '', 1),
        (175, '通知修改', 'system:announce:update', 3, 3, 171, '', '', '', 1),
        (176, '通知删除', 'system:announce:delete', 3, 4, 171, '', '', '', 1),
        (177, '通知发布', 'system:announce:publish', 3, 5, 171, '', '', '', 1),
        (178, '聊天查询', 'system:chat:query', 3, 1, 172, '', '', '', 1),
        (180, '定时任务', 'monitor:job:list', 2, 3, 100, '/monitor/job', 'Timer', 'monitor/job/index', 1),
        (181, '任务查询', 'monitor:job:query', 3, 1, 180, '', '', '', 1),
        (182, '任务新增', 'monitor:job:add', 3, 2, 180, '', '', '', 1),
        (183, '任务编辑', 'monitor:job:edit', 3, 3, 180, '', '', '', 1),
        (184, '任务删除', 'monitor:job:delete', 3, 4, 180, '', '', '', 1),
        (164, '代码生成', 'tool:gen:list', 2, 2, 150, '/tool/gen', 'SetUp', 'tool/gen/index', 1),
        (165, '代码生成查询', 'tool:gen:query', 3, 1, 164, '', '', '', 1),
        (166, '代码生成导入', 'tool:gen:import', 3, 2, 164, '', '', '', 1),
        (167, '代码生成修改', 'tool:gen:edit', 3, 3, 164, '', '', '', 1),
        (168, '代码生成删除', 'tool:gen:remove', 3, 4, 164, '', '', '', 1),
        (169, '代码生成预览', 'tool:gen:preview', 3, 5, 164, '', '', '', 1),
        (179, '代码生成执行', 'tool:gen:code', 3, 6, 164, '', '', '', 1)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name), permission = VALUES(permission), type = VALUES(type),
            sort = VALUES(sort), parent_id = VALUES(parent_id), path = VALUES(path),
            icon = VALUES(icon), component = VALUES(component), status = VALUES(status);

DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    ancestors VARCHAR(500) DEFAULT '' COMMENT '祖级列表，如 0,1,5',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    leader_name VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    leader_user_id BIGINT DEFAULT NULL COMMENT '负责人用户ID',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

INSERT INTO sys_dept (id, name, parent_id, ancestors, sort, status, leader_name, leader_user_id) VALUES
(1, '本部', 0, '0', 0, 1, '管理员', 1),
(2, '技术中心', 1, '0,1', 1, 1, NULL, NULL),
(3, '业务中心', 1, '0,1', 2, 1, NULL, NULL),
(4, '职能中心', 1, '0,1', 3, 1, NULL, NULL),
(5, '运营中心', 1, '0,1', 4, 1, NULL, NULL),
(6, '研发部', 2, '0,1,2', 1, 1, '张三', 2),
(7, '运维部', 2, '0,1,2', 2, 1, NULL, NULL),
(8, '产品部', 2, '0,1,2', 3, 1, NULL, NULL),
(9, '市场部', 3, '0,1,3', 1, 1, '李四', NULL),
(10, '财务部', 4, '0,1,4', 1, 1, '王五', NULL),
(11, '人事部', 4, '0,1,4', 2, 1, NULL, NULL),
(12, '实训部', 5, '0,1,5', 1, 1, NULL, NULL),
(13, '客服部', 5, '0,1,5', 2, 1, NULL, NULL),
(14, '前端组', 6, '0,1,2,6', 1, 1, NULL, NULL),
(15, '后端组', 6, '0,1,2,6', 2, 1, NULL, NULL);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '技术中心', 1, '0,1', 1, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '技术中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '业务中心', 1, '0,1', 2, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '业务中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '职能中心', 1, '0,1', 3, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '职能中心' AND parent_id = 1 AND deleted = 0);

INSERT INTO sys_dept (name, parent_id, ancestors, sort, status, leader_name)
SELECT '运营中心', 1, '0,1', 4, 1, NULL FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_dept WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE name = '运营中心' AND parent_id = 1 AND deleted = 0);

DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '岗位ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父岗位ID',
    post_code VARCHAR(50) NOT NULL COMMENT '岗位编码',
    post_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY uk_post_code (post_code),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

INSERT INTO sys_post (id, parent_id, post_code, post_name, sort, status, remark) VALUES
(1, 0, 'chairman', '董事长', 0, 1, '岗位体系根'),
(2, 1, 'ceo', '总经理', 1, 1, ''),
(3, 2, 'cto', '技术总监', 1, 1, ''),
(4, 2, 'dev_exec', '开发工程师', 2, 1, ''),
(5, 2, 'biz_line', '业务体系', 3, 1, '岗位分类'),
(6, 2, 'func_line', '职能体系', 4, 1, '岗位分类'),
(7, 2, 'ops_line', '运营体系', 5, 1, '岗位分类'),
(8, 3, 'dev', '研发工程师', 1, 1, ''),
(9, 8, 'dev_lead', '研发组长', 0, 1, ''),
(10, 8, 'fe_dev', '前端开发', 1, 1, ''),
(11, 8, 'be_dev', '后端开发', 2, 1, ''),
(12, 3, 'qa', '测试', 2, 1, ''),
(13, 12, 'qa_lead', '测试组长', 0, 1, ''),
(14, 3, 'product_mgr', '产品经理', 3, 1, ''),
(15, 3, 'ops_eng', '运维工程师', 4, 1, ''),
(16, 5, 'market_spec', '市场专员', 1, 1, ''),
(17, 6, 'finance_mgr', '财务主管', 1, 1, ''),
(18, 6, 'hr_spec', '人事专员', 2, 1, ''),
(19, 7, 'train_lecturer', '实训讲师', 1, 1, '');

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT 0, 'chairman', '董事长', 0, 1, '岗位体系根' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'chairman' AND deleted = 0);

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'cto', '技术总监', 1, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'cto' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'dev_exec', '开发工程师', 2, 1, ''
FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'dev_exec' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'biz_line', '业务体系', 3, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'biz_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'func_line', '职能体系', 4, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'func_line' AND deleted = 0)
LIMIT 1;

INSERT INTO sys_post (parent_id, post_code, post_name, sort, status, remark)
SELECT p.id, 'ops_line', '运营体系', 5, 1, '岗位分类' FROM sys_post p
WHERE p.post_code = 'ceo' AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_post WHERE post_code = 'ops_line' AND deleted = 0)
LIMIT 1;

DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE sys_user_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    post_id BIGINT NOT NULL COMMENT '岗位ID',
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id),
    UNIQUE KEY uk_user_post (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户岗位关联表';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), -- admin 拥有超级管理员角色
(2, 2);

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 130), (1, 160), (1, 163), (1, 191), (1, 192), (1, 193), (1, 7), (1, 8), (1, 9),
(1, 6), (1, 120), (1, 121), (1, 126), (1, 127), (1, 128),
(1, 10), (1, 11), (1, 12), (1, 13),
(1, 20), (1, 21), (1, 22), (1, 23),
(1, 30), (1, 31), (1, 32), (1, 33),
(1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 45), (1, 46), (1, 47),
(1, 131), (1, 132), (1, 133), (1, 134), (1, 135), (1, 161), (1, 162),
(1, 50), (1, 51), (1, 52),
(1, 60), (1, 61), (1, 62), (1, 63), (1, 64), (1, 65),
(1, 70), (1, 71), (1, 72), (1, 73), (1, 74),
(1, 100), (1, 101), (1, 102), (1, 103), (1, 190), (1, 104), (1, 180), (1, 181), (1, 182), (1, 183), (1, 184), (1, 185), (1, 186), (1, 187), (1, 188), (1, 189),
(1, 105), (1, 110), (1, 111), (1, 112), (1, 113),
(1, 150), (1, 151), (1, 164), (1, 165), (1, 166), (1, 167), (1, 168), (1, 169), (1, 179),
(1, 170), (1, 171), (1, 172), (1, 173), (1, 174), (1, 175), (1, 176), (1, 177), (1, 178),
(1, 210), (1, 200), (1, 201), (1, 202), (1, 203), (1, 204), (1, 205), (1, 206);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 2), (2, 10), (2, 3), (2, 20), (2, 4), (2, 30), (2, 5), (2, 40),
(2, 130), (2, 131), (2, 160), (2, 161),
(2, 8), (2, 7), (2, 60), (2, 61), (2, 62), (2, 63), (2, 64),
(2, 9), (2, 70), (2, 71), (2, 73),
(2, 101), (2, 102),
(2, 105), (2, 110), (2, 111), (2, 112),
(2, 121), (2, 126), (2, 6), (2, 50),
(2, 151),
(2, 170), (2, 172), (2, 178);

DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ipaddr VARCHAR(50) COMMENT 'IP地址',
    login_location VARCHAR(100) COMMENT '登录地点',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    status TINYINT DEFAULT 0 COMMENT '登录状态(0-成功 1-失败)',
    msg VARCHAR(255) COMMENT '消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(50) DEFAULT NULL COMMENT '模块标题',
    business_type INT DEFAULT 0 COMMENT '业务类型(0其它 1新增 2修改 3删除 4查询 5导出 6导入)',
    method VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
    request_method VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    oper_name VARCHAR(50) DEFAULT NULL COMMENT '操作人员',
    oper_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    oper_ip VARCHAR(128) DEFAULT NULL COMMENT '主机地址',
    oper_param VARCHAR(2000) DEFAULT NULL COMMENT '请求参数',
    json_result VARCHAR(2000) DEFAULT NULL COMMENT '返回参数',
    status INT DEFAULT 0 COMMENT '操作状态(0正常 1异常)',
    error_msg VARCHAR(2000) DEFAULT NULL COMMENT '错误消息',
    oper_time DATETIME DEFAULT NULL COMMENT '操作时间',
    cost_time BIGINT DEFAULT 0 COMMENT '消耗时间(ms)',
    PRIMARY KEY (id),
    INDEX idx_oper_time (oper_time),
    INDEX idx_oper_name (oper_name),
    INDEX idx_title (title),
    INDEX idx_oper_time_status (oper_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    status TINYINT DEFAULT 1 COMMENT '状态(0停用 1正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted INT DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(1, '系统状态', 'sys_normal_disable', 1, '通用启用停用'),
(2, '用户性别', 'sys_user_sex', 1, '用户性别'),
(3, '是否', 'sys_yes_no', 1, '是或否'),
(4, '工单状态', 'sys_ticket_status', 1, '工单流转状态'),
(5, '工单优先级', 'sys_ticket_priority', 1, '工单优先级'),
(6, '审批类型', 'sys_approval_form_type', 1, '审批单业务类型'),
(7, '审批状态', 'sys_approval_status', 1, '审批单流转状态');

INSERT INTO sys_dict_type (id, dict_name, dict_type, status, remark) VALUES
(4, '工单状态', 'sys_ticket_status', 1, '工单流转状态'),
(5, '工单优先级', 'sys_ticket_priority', 1, '工单优先级'),
(6, '审批类型', 'sys_approval_form_type', 1, '审批单业务类型'),
(7, '审批状态', 'sys_approval_status', 1, '审批单流转状态')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), remark = VALUES(remark);

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE sys_dict_data (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sort INT DEFAULT 0 COMMENT '排序',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典键值',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    css_class VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    list_class VARCHAR(100) DEFAULT NULL COMMENT '回显样式',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认(0否 1是)',
    status TINYINT DEFAULT 1 COMMENT '状态(0停用 1正常)',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted INT DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_value (dict_type, dict_value),  -- 不含 deleted；软删后同 value 再建需应用层处理
    KEY idx_dict_type (dict_type),
    KEY idx_dict_type_status_deleted (dict_type, status, deleted, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_normal_disable', 1, '启用', '1', 'success', 1, 1),
('sys_normal_disable', 2, '禁用', '0', 'danger', 0, 1),
('sys_user_sex', 1, '男', '1', 'primary', 0, 1),
('sys_user_sex', 2, '女', '2', 'danger', 0, 1),
('sys_user_sex', 3, '未知', '0', 'info', 1, 1),
('sys_yes_no', 1, '是', 'Y', 'success', 1, 1),
('sys_yes_no', 2, '否', 'N', 'info', 0, 1),
('sys_ticket_status', 1, '待处理', 'OPEN', 'info', 1, 1),
('sys_ticket_status', 2, '处理中', 'IN_PROGRESS', 'warning', 0, 1),
('sys_ticket_status', 3, '已解决', 'RESOLVED', 'success', 0, 1),
('sys_ticket_status', 4, '已关闭', 'CLOSED', 'danger', 0, 1),
('sys_ticket_priority', 1, '低', 'LOW', 'info', 0, 1),
('sys_ticket_priority', 2, '中', 'MEDIUM', 'success', 1, 1),
('sys_ticket_priority', 3, '高', 'HIGH', 'warning', 0, 1),
('sys_ticket_priority', 4, '紧急', 'URGENT', 'danger', 0, 1),
('sys_approval_form_type', 1, '通用', 'GENERAL', 'info', 1, 1),
('sys_approval_form_type', 2, '请假', 'LEAVE', 'primary', 0, 1),
('sys_approval_form_type', 3, '采购', 'PURCHASE', 'warning', 0, 1),
('sys_approval_form_type', 4, '报销', 'REIMBURSE', 'success', 0, 1),
('sys_approval_form_type', 5, '用印', 'SEAL', 'danger', 0, 1),
('sys_approval_form_type', 6, '合同', 'CONTRACT', 'info', 0, 1),
('sys_approval_form_type', 7, '注册审核', 'REGISTER', 'warning', 0, 1),
('sys_approval_status', 1, '待审批', 'SUBMITTED', 'warning', 1, 1),
('sys_approval_status', 2, '已通过', 'APPROVED', 'success', 0, 1),
('sys_approval_status', 3, '已驳回', 'REJECTED', 'danger', 0, 1),
('sys_approval_status', 4, '已归档', 'ARCHIVED', 'info', 0, 1);

INSERT INTO sys_dict_data (dict_type, sort, dict_label, dict_value, list_class, is_default, status) VALUES
('sys_ticket_status', 1, '待处理', 'OPEN', 'info', 1, 1),
('sys_ticket_status', 2, '处理中', 'IN_PROGRESS', 'warning', 0, 1),
('sys_ticket_status', 3, '已解决', 'RESOLVED', 'success', 0, 1),
('sys_ticket_status', 4, '已关闭', 'CLOSED', 'danger', 0, 1),
('sys_ticket_priority', 1, '低', 'LOW', 'info', 0, 1),
('sys_ticket_priority', 2, '中', 'MEDIUM', 'success', 1, 1),
('sys_ticket_priority', 3, '高', 'HIGH', 'warning', 0, 1),
('sys_ticket_priority', 4, '紧急', 'URGENT', 'danger', 0, 1),
('sys_approval_form_type', 1, '通用', 'GENERAL', 'info', 1, 1),
('sys_approval_form_type', 2, '请假', 'LEAVE', 'primary', 0, 1),
('sys_approval_form_type', 3, '采购', 'PURCHASE', 'warning', 0, 1),
('sys_approval_form_type', 4, '报销', 'REIMBURSE', 'success', 0, 1),
('sys_approval_form_type', 5, '用印', 'SEAL', 'danger', 0, 1),
('sys_approval_form_type', 6, '合同', 'CONTRACT', 'info', 0, 1),
('sys_approval_form_type', 7, '注册审核', 'REGISTER', 'warning', 0, 1),
('sys_approval_status', 1, '待审批', 'SUBMITTED', 'warning', 1, 1),
('sys_approval_status', 2, '已通过', 'APPROVED', 'success', 0, 1),
('sys_approval_status', 3, '已驳回', 'REJECTED', 'danger', 0, 1),
('sys_approval_status', 4, '已归档', 'ARCHIVED', 'info', 0, 1)
ON DUPLICATE KEY UPDATE
    dict_label = VALUES(dict_label),
    list_class = VALUES(list_class),
    is_default = VALUES(is_default),
    sort = VALUES(sort);

DROP TABLE IF EXISTS `sys_config_group`;
CREATE TABLE IF NOT EXISTS sys_config_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    group_code VARCHAR(50) NOT NULL COMMENT '分组编码',
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    config_value TEXT NOT NULL COMMENT 'JSON 配置',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator VARCHAR(64) DEFAULT '',
    updater VARCHAR(64) DEFAULT '',
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置分组';

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('site', '基础信息', '{"platformName":"Admin Platform","platformSubtitle":"统一运维 · 高效管控","loginWelcome":"Welcome","registerTitle":"Sign Up","copyright":""}', '平台展示名称与登录页文案'),
('session', '会话配置', '{"tokenExpireHours":24,"sessionSignExpireHours":24}', 'JWT 与 Redis 会话有效期（小时）'),
('file', '文件配置', '{"maxSizeMb":50,"allowedExtensions":"jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov"}', '文件管理上传限制'),
('rateLimit', '接口限流', '{"captchaPerIpMinute":40,"loginPerIpMinute":30,"registerPerIpMinute":10,"smsPerIpMinute":5,"smsSendIntervalSeconds":60,"smsPerPhoneDaily":10,"smsPerIpDaily":30}', '认证接口按 IP 限流；含短信防刷'),
('login', '登录配置', '{"captchaEnabled":true,"captchaType":"image","smsLoginEnabled":false,"smsLoginSliderCaptchaEnabled":false,"rememberMe":true,"maxRetryCount":5,"maxRetryCountIp":20,"lockTime":10}', '验证码 image/slider；smsLoginEnabled 短信登录；smsLoginSliderCaptchaEnabled 短信发送前滑块；maxRetryCount 账号锁定阈值；maxRetryCountIp IP 锁定阈值'),
('register', '注册配置', '{"enabled":true,"captchaEnabled":true,"captchaType":"image","defaultRoleCode":"user","needAudit":false,"minPasswordLength":6}', '开放注册、验证码类型、默认角色、是否审核'),
('thirdParty', '第三方配置', '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}', '微信/支付宝/GitHub/Google 第三方登录'),
('payment', '支付配置', '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}', '微信/支付宝支付与测试下单'),
('sms', '短信配置', '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}', '阿里云短信认证/腾讯云'),
('email', '邮件配置', '{"enabled":true,"provider":"qq","host":"smtp.qq.com","port":465,"username":"974473458@qq.com","password":"cqjvfpulydqwbegh","fromName":"wu-admin 系统团队","authEnabled":true,"securityType":"SSL","connectionTimeoutMs":5000,"timeoutMs":5000,"writeTimeoutMs":5000,"encoding":"UTF-8","debug":false,"codeExpireMinutes":5,"codeLength":6,"dailyLimitPerEmail":20,"sendIntervalSeconds":60}', '企业级 SMTP 邮件服务配置：发件人、SSL端口、超时限额与验证码防刷规则'),
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false,"sm4EncryptEnabled":true,"sm2SignEnabled":true,"sm3SignEnabled":true,"timestampEnabled":true,"nonceEnabled":true}', '安全防线与会话：SM4加密（会话密钥）、HMAC-SM3数字签名、时间戳与Nonce防重放');

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('thirdParty', '第三方配置',
 '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}',
 '微信/支付宝/GitHub/Google 第三方登录密钥'),
('payment', '支付配置',
 '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}',
 '微信/支付宝支付与测试下单'),
('ai', 'AI助手配置',
 '{"globalKnowledge":"","answerScope":"focus","tokensPerUserDaily":100000,"roleTokenQuotas":[]}',
 'AI wu助手配置：answerScope 回答边界(focus/open)、Token 每日配额设置')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('security', '安全配置', '{"disableDevtool":false,"isConcurrent":false}', '前端安全与会话：禁止调试、禁止多端同时在线')
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    remark = VALUES(remark);

INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('sms', '短信配置', '{"enabled":false,"provider":"aliyunAuth","accessKeyId":"","accessKeySecret":"","signName":"","tencentAppId":"","templateVerifyCode":"100001","templateModifyPhone":"100002","templateResetPassword":"100003","templateBindPhone":"100004","templateVerifyBindPhone":"100005","schemeName":"","codeExpireMinutes":5}', '阿里云短信认证/腾讯云')
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name), remark = VALUES(remark);

SET FOREIGN_KEY_CHECKS = 1;
