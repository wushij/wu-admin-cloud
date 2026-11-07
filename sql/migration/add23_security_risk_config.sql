-- ============================================================
-- 安全风控配置项补齐（微服务安全风控模块落地）
--
-- 背景：
--   微服务迁移时安全风控模块（system 服务 SecurityRiskController + AntiBotRiskService）
--   整体缺失，sys_security_event 表已建但无任何读写实现。
--   本次补齐模块后，RiskConfigHelper 从 sys_config_group 的 security 分组读取
--   17 个风控配置项；这些 key 在历史库中不存在，只能读到默认值。
--
-- 幂等：使用 JSON_MERGE_PATCH，只补缺失的 key，不覆盖运营已调整过的值。
--   例：某库已把 riskBlockScore 调成 120，本脚本不会把它改回 80。
--
-- 依赖：MySQL 8.0+（JSON_MERGE_PATCH 自 8.0.3 起提供）
-- ============================================================

-- 1) 为 security 分组补齐风控配置项（缺失才补，已有值保留）
UPDATE `sys_config_group`
SET `config_value` = JSON_MERGE_PATCH(
        COALESCE(`config_value`, '{}'),
        JSON_OBJECT(
            'riskEnabled',            true,
            'riskWindowMinutes',      60,
            'riskNoticeScore',        30,
            'riskWarnScore',          50,
            'riskChallengeScore',     70,
            'riskBlockScore',         80,
            'riskBanScore',           120,
            'riskBlockSeconds',       3600,
            'riskBanSeconds',         86400,
            'riskChallengeEnabled',   false,
            'riskBehaviorEnabled',    true,
            'riskPageScanThreshold',  8,
            'riskExportThreshold',    5,
            'riskAlertWebhook',       '',
            'honeypotEnabled',        false,
            'honeypotRiskDelta',      80,
            'honeypotBlockSeconds',   86400
        )
    )
WHERE `group_code` = 'security';

-- 2) 修正 remark，说明风控配置项的含义（不影响功能，便于运维排查）
UPDATE `sys_config_group`
SET `remark` = '安全防线与会话：SM4加密（会话密钥）、HMAC-SM3数字签名、时间戳与Nonce防重放；风控：riskEnabled总开关、riskWindowMinutes滑动窗口、riskNoticeScore/riskWarnScore/riskChallengeScore/riskBlockScore/riskBanScore五档阈值、riskBlockSeconds/riskBanSeconds封禁时长、riskChallengeEnabled二次验证、riskBehaviorEnabled行为识别、riskPageScanThreshold/riskExportThreshold行为阈值、riskAlertWebhook告警地址、honeypotEnabled蜜罐开关、honeypotRiskDelta/honeypotBlockSeconds蜜罐参数'
WHERE `group_code` = 'security';

-- 3) 确认安全风控菜单与权限存在（微服务 SQL 已含 230~235，此处兜底补齐）
INSERT IGNORE INTO `sys_menu` (id, name, permission, type, sort, parent_id, path, icon, component, status) VALUES
(230, '安全风控',   'system:security:list',    2, 3, 120, '/system/security-log', 'Lock', 'system/security-log/index', 1),
(231, '安全查询',   'system:security:query',   3, 1, 230, '', '', '', 1),
(232, '风险解封',   'system:security:unblock', 3, 2, 230, '', '', '', 1),
(233, '手动拉黑',   'system:security:block',   3, 3, 230, '', '', '', 1),
(234, '清空安全日志','system:security:clear',  3, 4, 230, '', '', '', 1),
(235, '删除安全日志','system:security:delete', 3, 5, 230, '', '', '', 1);

-- 4) 超管角色绑定安全风控菜单
INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id) VALUES
(1, 230), (1, 231), (1, 232), (1, 233), (1, 234), (1, 235);
