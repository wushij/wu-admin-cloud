-- =============================================================================
-- add24 · 同步单体最新菜单、现代科技图标、AI知识库及普通用户数据范围
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `wu_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu_system`;

SET NAMES utf8mb4;

-- 1. 补全缺失的 AI 知识库与按钮权限
INSERT INTO `sys_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`) VALUES
(207, 'AI模型查询', 'system:ai-model:query', 3, 1, 200, '', '', '', 1),
(208, 'AI日志查询', 'system:ai-log:query', 3, 1, 205, '', '', '', 1),
(220, 'AI 知识库', 'system:ai-knowledge:list', 2, 3, 210, '/ai/knowledge', 'Reading', 'ai/knowledge/index', 1),
(221, 'AI知识新增', 'system:ai-knowledge:create', 3, 2, 220, '', '', '', 1),
(222, 'AI知识修改', 'system:ai-knowledge:update', 3, 3, 220, '', '', '', 1),
(223, 'AI知识删除', 'system:ai-knowledge:delete', 3, 4, 220, '', '', '', 1),
(224, 'AI知识查询', 'system:ai-knowledge:query', 3, 1, 220, '', '', '', 1)
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`permission` = VALUES(`permission`),
`type` = VALUES(`type`),
`sort` = VALUES(`sort`),
`parent_id` = VALUES(`parent_id`),
`path` = VALUES(`path`),
`icon` = VALUES(`icon`),
`component` = VALUES(`component`),
`status` = VALUES(`status`);

-- 2. 菜单图标科技感升级
UPDATE `sys_menu` SET `icon` = 'Workflow' WHERE `id` = 8 OR `path` = '/workflow';
UPDATE `sys_menu` SET `icon` = 'Calendar' WHERE `id` = 120 OR `path` = '/log';
UPDATE `sys_menu` SET `icon` = 'Odometer' WHERE `id` = 187 OR `path` = '/monitor/server';
UPDATE `sys_menu` SET `icon` = 'AiModel' WHERE `id` = 200 OR `path` = '/ai/model';
UPDATE `sys_menu` SET `icon` = 'Compass' WHERE `id` = 210 OR `path` = '/ai';
UPDATE `sys_menu` SET `icon` = 'Reading' WHERE `id` = 220 OR `path` = '/ai/knowledge';
UPDATE `sys_menu` SET `icon` = 'Lock' WHERE `id` = 230 OR `path` = '/system/security-log';

-- 3. 子菜单顺序对齐（用户 -> 角色 -> 菜单 -> 组织 -> 字典 -> 回收 -> 配置）
UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 2;
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 3;
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 4;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 5;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 130;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 163;
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 160;

-- 4. 纠偏普通用户数据范围（data_scope=4: 本部门及以下）
UPDATE `sys_role` SET `data_scope` = 4 WHERE `id` = 2 OR `code` = 'user';

-- 5. 权限绑定（幂等）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 207), (1, 208), (1, 220), (1, 221), (1, 222), (1, 223), (1, 224),
(2, 207), (2, 208), (2, 220), (2, 221), (2, 222), (2, 223), (2, 224);

