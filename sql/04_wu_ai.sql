-- =============================================================================
-- wu-service-ai 库脚本（库名 wu-admin，可单独部署到独立 MySQL）
-- =============================================================================
CREATE DATABASE IF NOT EXISTS `wu-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wu-admin`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_ai_model`;
CREATE TABLE IF NOT EXISTS sys_ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型配置ID',
    name VARCHAR(100) NOT NULL COMMENT '配置名称（展示用，如「DeepSeek 官方」）',
    provider VARCHAR(32) NOT NULL COMMENT '供应商: deepseek/openai/qwen/kimi',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称（如 deepseek-chat / qwen-plus）',
    base_url VARCHAR(255) NOT NULL COMMENT 'API 基础地址（不含 /chat/completions）',
    api_key VARCHAR(1024) DEFAULT '' COMMENT 'API 密钥（SM4-CBC 加密存储）',
    temperature DECIMAL(3,2) DEFAULT 0.70 COMMENT '采样温度 0~2',
    max_tokens INT DEFAULT 4096 COMMENT '单次回复最大 Token 数',
    system_prompt VARCHAR(2000) DEFAULT NULL COMMENT '系统提示词（角色设定）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模型 0:否 1:是（全局唯一）',
    status TINYINT DEFAULT 1 COMMENT '状态 0:禁用 1:启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_provider (provider),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型供应商配置表';

DROP TABLE IF EXISTS `sys_ai_chat_log`;
CREATE TABLE IF NOT EXISTS sys_ai_chat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT DEFAULT 0 COMMENT '提问用户ID',
    username VARCHAR(64) DEFAULT '' COMMENT '提问用户名',
    conversation_id VARCHAR(64) DEFAULT '' COMMENT '会话ID（前端生成，串联多轮对话）',
    model_id BIGINT DEFAULT 0 COMMENT '模型配置ID',
    provider VARCHAR(32) DEFAULT '' COMMENT '供应商',
    model_name VARCHAR(100) DEFAULT '' COMMENT '模型名称',
    question TEXT COMMENT '用户提问（脱敏后）',
    answer MEDIUMTEXT COMMENT 'AI 回答',
    prompt_tokens INT DEFAULT 0 COMMENT '提问 Token 消耗',
    completion_tokens INT DEFAULT 0 COMMENT '回答 Token 消耗',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    duration_ms BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    chat_status TINYINT DEFAULT 1 COMMENT '结果 1:成功 0:失败 2:用户中断',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    source VARCHAR(20) DEFAULT 'pc' COMMENT '来源终端 pc/mobile',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_id (user_id),
    INDEX idx_model_id (model_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话日志审计表';

DROP TABLE IF EXISTS `sys_ai_knowledge`;
CREATE TABLE IF NOT EXISTS sys_ai_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识ID',
    title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '知识标题',
    keywords VARCHAR(255) NOT NULL DEFAULT '' COMMENT '检索关键词(逗号分隔)',
    content TEXT NOT NULL COMMENT '知识正文(Markdown)',
    category VARCHAR(32) NOT NULL DEFAULT 'faq' COMMENT '分类: faq/manual/module/other',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序权重(大者优先)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_status_category (status, category),
    FULLTEXT INDEX ft_knowledge (title, keywords, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库';

SET FOREIGN_KEY_CHECKS = 1;
