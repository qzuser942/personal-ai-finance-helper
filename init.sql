-- ============================================================
-- = 个人理财助手应用系统 — 最终完整数据库初始化脚本
-- = 数据库名：finance_db
-- = 字符集：utf8mb4 | 排序规则：utf8mb4_unicode_ci
-- = 存储引擎：InnoDB
-- = 版本：V1.0 Final | 日期：2026-06-23
-- = 编制人：胡宪棋 | 学号：202421332084 | 班级：软件2413
-- = 基于：SRS + API接口文档 + 数据库设计说明书 + 后端实体类代码
-- ============================================================

-- ============================================================
-- Part 0: 建库 & 上下文切换
-- ============================================================
DROP DATABASE IF EXISTS `finance_db`;
CREATE DATABASE `finance_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `finance_db`;

-- ============================================================
-- Part 1: DDL — 全部10张数据表
-- 建表顺序遵守外键依赖：sys_user / sys_admin → category → bill → budget / save_target / ai_analysis_record
-- admin_operation_log / ai_config / database_backup_log 为独立表，无外键
-- ============================================================

-- --------------------------------------------
-- 1.1 sys_user — 普通用户表
-- 用途：存储鸿蒙手机客户端注册的普通用户账号信息
-- --------------------------------------------
CREATE TABLE `sys_user` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `username`        VARCHAR(64)   NOT NULL COMMENT '登录用户名（4-20字符，字母数字下划线）',
    `password`        VARCHAR(255)  NOT NULL COMMENT 'BCrypt加密后的密码哈希值',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，0-冻结',
    `last_login_time` DATETIME      DEFAULT NULL COMMENT '最近一次登录时间',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号注册时间',
    `updated_at`      DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';

-- --------------------------------------------
-- 1.2 sys_admin — 管理员表
-- 用途：存储Vue管理员后台的管理员账号信息。与普通用户物理分表。
-- --------------------------------------------
CREATE TABLE `sys_admin` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '管理员主键ID',
    `username`        VARCHAR(64)   NOT NULL COMMENT '管理员登录账号',
    `password`        VARCHAR(255)  NOT NULL COMMENT 'BCrypt加密后的密码哈希值',
    `role`            VARCHAR(32)   NOT NULL DEFAULT 'OPERATOR' COMMENT '角色权限：SUPER_ADMIN-超级管理员，OPERATOR-运营管理员',
    `last_login_time` DATETIME      DEFAULT NULL COMMENT '最近一次登录时间',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    `updated_at`      DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- --------------------------------------------
-- 1.3 category — 消费分类表
-- 先于 bill 建表，因 bill 通过外键引用 category.id
-- user_id=0 表示系统内置分类（全局可见）；user_id>0 表示用户自定义分类（仅该用户可见）
-- --------------------------------------------
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
    `user_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '所属用户ID：0-系统内置分类，>0-用户自定义分类',
    `name`        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `icon`        VARCHAR(128) DEFAULT NULL COMMENT '分类图标标识/图标名称（前端映射）',
    `type`        VARCHAR(16)  NOT NULL COMMENT '分类类型：income-收入分类，expense-支出分类',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序序号（升序排列，数字越小越靠前）',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name_type` (`user_id`, `name`, `type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费分类表';

-- --------------------------------------------
-- 1.4 bill — 账单表（核心业务表）
-- 每条账单关联用户（user_id）和消费分类（category_id）。
-- sync_uuid 唯一键支持离线同步去重。
-- 金额统一使用 DECIMAL(12,2) 保障财务精度。
-- --------------------------------------------
CREATE TABLE `bill` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '账单主键ID',
    `user_id`         BIGINT         NOT NULL COMMENT '所属用户ID，外键关联sys_user.id',
    `amount`          DECIMAL(12,2)  NOT NULL COMMENT '金额（正数，收支类型由type字段区分）',
    `type`            VARCHAR(16)    NOT NULL COMMENT '收支类型：income-收入，expense-支出',
    `category_id`     BIGINT         NOT NULL COMMENT '消费分类ID，外键关联category.id',
    `remark`          VARCHAR(500)   DEFAULT NULL COMMENT '文字备注（最大500字符）',
    `receipt_image`   VARCHAR(500)   DEFAULT NULL COMMENT '小票图片服务端相对路径（如/uploads/receipts/xxx.jpg）',
    `sync_uuid`       VARCHAR(36)    DEFAULT NULL COMMENT '离线同步UUID v4，用于去重；在线创建为NULL',
    `consume_time`    DATETIME       NOT NULL COMMENT '消费/收入实际发生时间',
    `is_deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账单记录创建时间',
    `updated_at`      DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '账单最近修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_type` (`type`),
    KEY `idx_consume_time` (`consume_time`),
    KEY `idx_user_consume` (`user_id`, `consume_time`),
    KEY `idx_user_type_time` (`user_id`, `type`, `consume_time`),
    KEY `idx_is_deleted` (`is_deleted`),
    UNIQUE KEY `uk_sync_uuid` (`sync_uuid`),
    CONSTRAINT `fk_bill_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_bill_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单表';

-- --------------------------------------------
-- 1.5 budget — 月度预算表
-- 每用户每月至多一条预算记录（uk_user_yearmonth 保证）。
-- 字段名用 yearmonth 而非 year_month 是为避免与 MySQL 8 保留字冲突。
-- category_budgets 使用 MySQL 原生 JSON 类型存储分类子预算。
-- --------------------------------------------
CREATE TABLE `budget` (
    `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '预算主键ID',
    `user_id`           BIGINT         NOT NULL COMMENT '所属用户ID，外键关联sys_user.id',
    `yearmonth`         VARCHAR(7)     NOT NULL COMMENT '预算所属月份，格式YYYY-MM（如2026-06）',
    `total_budget`      DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '月度总预算金额',
    `category_budgets`  JSON           DEFAULT NULL COMMENT '各分类子预算JSON：{"category_id": 金额, ...}',
    `is_deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_yearmonth` (`user_id`, `yearmonth`),
    KEY `idx_is_deleted` (`is_deleted`),
    CONSTRAINT `fk_budget_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度预算表';

-- --------------------------------------------
-- 1.6 save_target — 存钱目标表
-- 支持追踪存入进度、自动完成标记；当 saved_amount >= target_amount 时 status 自动变更为 1。
-- --------------------------------------------
CREATE TABLE `save_target` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '目标主键ID',
    `user_id`         BIGINT         NOT NULL COMMENT '所属用户ID，外键关联sys_user.id',
    `name`            VARCHAR(128)   NOT NULL COMMENT '目标名称（如"买MacBook Pro"）',
    `target_amount`   DECIMAL(12,2)  NOT NULL COMMENT '目标总金额',
    `saved_amount`    DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '当前已存金额',
    `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '完成状态：0-进行中，1-已完成',
    `completed_at`    DATETIME       DEFAULT NULL COMMENT '目标达成时间（status变为1时自动记录）',
    `is_deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '目标创建时间',
    `updated_at`      DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_is_deleted` (`is_deleted`),
    CONSTRAINT `fk_target_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存钱目标表';

-- --------------------------------------------
-- 1.7 admin_operation_log — 管理员操作日志表
-- 只追加表，不建外键约束（避免级联删除导致日志丢失）。
-- 通过 AOP 切面（AdminLogAspect）自动采集，管理员无感知。
-- --------------------------------------------
CREATE TABLE `admin_operation_log` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
    `admin_id`       BIGINT        NOT NULL COMMENT '操作管理员ID',
    `admin_username` VARCHAR(64)   NOT NULL COMMENT '操作管理员账号（冗余字段，提高查询效率）',
    `operation`      VARCHAR(128)  NOT NULL COMMENT '操作类型描述（如"删除账单"、"冻结用户"、"修改分类"）',
    `method`         VARCHAR(16)   NOT NULL COMMENT 'HTTP请求方法：POST/PUT/DELETE',
    `request_url`    VARCHAR(255)  NOT NULL COMMENT '请求接口路径（如/api/admin/bill/123）',
    `request_params` TEXT          DEFAULT NULL COMMENT '请求参数（JSON序列化，敏感字段已脱敏）',
    `ip_address`     VARCHAR(64)   DEFAULT NULL COMMENT '操作者IP地址',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '操作结果：1-成功，0-失败',
    `error_msg`      VARCHAR(1000) DEFAULT NULL COMMENT '操作失败时的错误信息',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `updated_at`     DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_operation` (`operation`),
    KEY `idx_admin_time` (`admin_id`, `created_at`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- --------------------------------------------
-- 1.8 ai_analysis_record — AI分析记录表
-- 持久化存储每次AI理财分析的结果。result_json 使用 MEDIUMTEXT（最大16MB）。
-- prompt_template_snapshot 记录分析时实际使用的Prompt，确保历史可回溯。
-- --------------------------------------------
CREATE TABLE `ai_analysis_record` (
    `id`                       BIGINT     NOT NULL AUTO_INCREMENT COMMENT '分析记录主键ID',
    `user_id`                  BIGINT     NOT NULL COMMENT '所属用户ID，外键关联sys_user.id',
    `yearmonth`                VARCHAR(7) NOT NULL COMMENT '分析账单所属月份，格式YYYY-MM',
    `result_json`              MEDIUMTEXT NOT NULL COMMENT 'AI分析结果JSON（含overview/wasteItems/badHabits/suggestions/nextMonthPlan）',
    `prompt_template_snapshot` TEXT       DEFAULT NULL COMMENT '本次分析使用的Prompt模板文本快照（用于回溯）',
    `model_name`               VARCHAR(64) NOT NULL DEFAULT 'DeepSeek' COMMENT '调用的大模型名称',
    `processing_time_ms`       BIGINT     DEFAULT NULL COMMENT 'AI处理耗时（毫秒），用于性能监控',
    `is_deleted`               TINYINT    NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`               DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析完成时间',
    `updated_at`               DATETIME   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_yearmonth` (`yearmonth`),
    KEY `idx_user_month` (`user_id`, `yearmonth`),
    KEY `idx_is_deleted` (`is_deleted`),
    CONSTRAINT `fk_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析记录表';

-- --------------------------------------------
-- 1.9 ai_config — AI配置表
-- 独立配置表，无外键。key-value模式支持管理员在线热更新。
-- 配置变更即时生效，无需重启服务。
-- --------------------------------------------
CREATE TABLE `ai_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键ID',
    `config_key`  VARCHAR(128) NOT NULL COMMENT '配置键名（如deepseek_api_key、prompt_template_analysis）',
    `config_value` TEXT        NOT NULL COMMENT '配置值（文本内容、数字参数、JSON等）',
    `config_type` VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT '配置值类型：STRING-字符串，NUMBER-数字，JSON-JSON，TEXT-长文本',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '配置项说明描述',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配置表';

-- --------------------------------------------
-- 1.10 database_backup_log — 数据库备份日志表
-- 只追加表，不建外键约束。记录历次备份操作信息。
-- --------------------------------------------
CREATE TABLE `database_backup_log` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '备份日志主键ID',
    `admin_id`   BIGINT        NOT NULL COMMENT '执行备份的管理员ID',
    `file_name`  VARCHAR(255)  NOT NULL COMMENT '备份文件名（如backup_20260623_143000.sql）',
    `file_path`  VARCHAR(500)  NOT NULL COMMENT '备份文件完整路径',
    `file_size`  BIGINT        DEFAULT NULL COMMENT '备份文件大小（字节）',
    `status`     TINYINT       NOT NULL DEFAULT 1 COMMENT '备份结果：1-成功，0-失败',
    `error_msg`  VARCHAR(1000) DEFAULT NULL COMMENT '备份失败时的错误信息',
    `is_deleted` TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份执行时间',
    `updated_at` DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库备份日志表';


-- ============================================================
-- Part 2: DML — 初始化数据
-- 数据来源：DataInitializer.java + CategoryServiceImpl.initSystemCategories()
-- 所有 INSERT 均带幂等性逻辑（NOT EXISTS / IGNORE）
-- ============================================================

-- --------------------------------------------
-- 2.1 系统内置支出分类（user_id=0, type='expense'）
-- 共16条
-- --------------------------------------------
INSERT INTO `category` (`user_id`, `name`, `icon`, `type`, `sort_order`) VALUES
(0, '餐饮',   'icon-food',           'expense', 1),
(0, '交通',   'icon-transport',      'expense', 2),
(0, '购物',   'icon-shopping',       'expense', 3),
(0, '娱乐',   'icon-entertainment',  'expense', 4),
(0, '住房',   'icon-housing',        'expense', 5),
(0, '医疗',   'icon-medical',        'expense', 6),
(0, '教育',   'icon-education',      'expense', 7),
(0, '通讯',   'icon-communication',  'expense', 8),
(0, '服饰',   'icon-clothing',       'expense', 9),
(0, '日用品', 'icon-daily',          'expense', 10),
(0, '丽人',   'icon-beauty',         'expense', 11),
(0, '运动',   'icon-sports',         'expense', 12),
(0, '旅行',   'icon-travel',         'expense', 13),
(0, '宠物',   'icon-pet',            'expense', 14),
(0, '数码',   'icon-digital',        'expense', 15),
(0, '其他支出','icon-other-expense',  'expense', 99);

-- --------------------------------------------
-- 2.2 系统内置收入分类（user_id=0, type='income'）
-- 共7条
-- --------------------------------------------
INSERT INTO `category` (`user_id`, `name`, `icon`, `type`, `sort_order`) VALUES
(0, '工资',     'icon-salary',       'income', 1),
(0, '奖金',     'icon-bonus',        'income', 2),
(0, '投资收益', 'icon-investment',    'income', 3),
(0, '兼职',     'icon-parttime',     'income', 4),
(0, '红包',     'icon-redpacket',    'income', 5),
(0, '退款',     'icon-refund',       'income', 6),
(0, '其他收入', 'icon-other-income',  'income', 99);

-- --------------------------------------------
-- 2.3 超级管理员初始化
-- 默认账号：admin / 密码：admin123
-- 下方密文为 admin123 的真实 BCrypt 哈希（由 BCryptPasswordEncoder 强度 10 生成）
-- 部署后请立即通过「管理员管理 → 重置密码」修改默认密码
-- --------------------------------------------
INSERT INTO `sys_admin` (`username`, `password`, `role`) VALUES
('admin', '$2b$10$mabhj31G4lzA1rTytYqV3erPLZdTTCekr9ZkBHeQMQAFBb1LOVWeG', 'SUPER_ADMIN');

-- --------------------------------------------
-- 2.4 AI 默认配置初始化（共15条）
-- 包含 DeepSeek 大模型配置、阿里云百炼嵌入配置、
-- Qdrant 向量数据库配置、Prompt 模板
-- --------------------------------------------

-- === DeepSeek 大模型配置 ===
INSERT INTO `ai_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('deepseek_api_key',  '${DEEPSEEK_API_KEY:sk-your-deepseek-api-key}', 'STRING',
 'DeepSeek官方API密钥（通过环境变量DEEPSEEK_API_KEY配置）'),

('model_base_url', 'https://api.deepseek.com/v1', 'STRING',
 'DeepSeek官方API地址（OpenAI兼容接口）'),

('model_name',        'deepseek-chat', 'STRING',
 '大模型名称：deepseek-chat / deepseek-reasoner'),

('model_temperature', '0.7', 'NUMBER',
 '模型temperature参数（0-2），控制输出随机性'),

('model_max_tokens',  '4096', 'NUMBER',
 '模型max_tokens参数，最大输出长度'),

('model_top_p',       '0.9', 'NUMBER',
 '模型top_p核采样参数');

-- === 阿里云百炼嵌入配置 ===
INSERT INTO `ai_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('dashscope_api_key',   '${DASHSCOPE_API_KEY:sk-your-dashscope-api-key}', 'STRING',
 '阿里云百炼DashScope API密钥（通过环境变量DASHSCOPE_API_KEY配置）'),

('embedding_model',     'text-embedding-v2', 'STRING',
 '阿里云百炼文本嵌入模型名称'),

('embedding_dimension', '1536', 'NUMBER',
 '向量维度（text-embedding-v2 = 1536）');

-- === Qdrant向量数据库配置 ===
INSERT INTO `ai_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('qdrant_host',       'localhost', 'STRING',
 'Qdrant向量数据库服务地址'),

('qdrant_port',       '6334', 'NUMBER',
 'Qdrant gRPC通信端口'),

('qdrant_collection', 'user_consumption_vectors', 'STRING',
 'Qdrant向量集合名称');

-- === Prompt 模板 ===
INSERT INTO `ai_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('prompt_template_analysis',
 '你是一名专业的个人理财顾问，持有CFP认证。\n请基于以下用户月度账单数据进行全面的财务诊断分析。\n\n{{markdown_bill_data}}\n\n请严格按以下JSON格式返回（不要包含```json```代码块）：\n{\n  "overview": {"totalIncome": 0, "totalExpense": 0, "balance": 0, "healthScore": 0, "summary": ""},\n  "wasteItems": [{"name": "", "amount": 0, "category": "", "reason": "", "suggestion": "", "severity": "MEDIUM"}],\n  "badHabits": [{"habit": "", "description": "", "impact": "", "severity": "MEDIUM"}],\n  "suggestions": [{"plan": "", "description": "", "estimatedMonthlySave": "", "difficulty": "MODERATE"}],\n  "nextMonthPlan": {"totalBudget": 0, "categoryAllocations": {}, "tips": []}\n}',
 'TEXT',
 'AI月度账单分析Prompt模板，{{markdown_bill_data}}为账单数据占位符'),

('prompt_template_classify',
 '你是一个智能消费分类助手。根据消费备注自动匹配最合适的分类。\n\n{{markdown_classify_data}}\n\n返回JSON格式（Top3备选）：\n{"categoryName": "", "confidence": 0.0, "reason": "", "top3Alternatives": [{"categoryName": "", "confidence": 0.0}]}',
 'TEXT',
 'AI消费分类推荐Prompt模板'),

('prompt_template_feature_extraction',
 '从以下AI诊断报告中提取用户消费行为特征。\n{{diagnosis_summary}}\n\n返回2-3句简洁的特征描述文本（50-100字）。',
 'TEXT',
 'AI消费特征提取Prompt模板（用于Qdrant向量存储）');


-- ============================================================
-- Part 3: 数据验证查询（可选执行）
-- 取消注释以验证初始化结果
-- ============================================================

-- SELECT '===== 数据表列表 =====' AS '';
-- SHOW TABLES;
--
-- SELECT '===== 系统分类（共' || COUNT(*) || '条）=====' AS '' FROM category WHERE user_id = 0;
-- SELECT id, name, type, sort_order FROM category WHERE user_id = 0 ORDER BY type, sort_order;
--
-- SELECT '===== 管理员 =====' AS '';
-- SELECT id, username, role FROM sys_admin WHERE is_deleted = 0;
--
-- SELECT '===== AI配置（共' || COUNT(*) || '条）=====' AS '' FROM ai_config WHERE is_deleted = 0;
-- SELECT id, config_key, config_type, LEFT(config_value, 60) AS value_preview FROM ai_config WHERE is_deleted = 0;

-- ============================================================
-- End of init.sql — 个人理财助手应用系统 V1.0 Final
-- ============================================================