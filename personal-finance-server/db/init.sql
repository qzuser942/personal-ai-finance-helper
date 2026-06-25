-- ============================================================
-- 智能理财管理系统 - 数据库初始化脚本
-- 版本: v2.0 (含 admin_operation_log 新增 admin_role + resource_id)
-- 数据库: finance_db
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `finance_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `finance_db`;

-- ------------------------------------------------------------
-- 1. 普通用户表 sys_user
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `username`        VARCHAR(64)  NOT NULL                COMMENT '登录用户名（4-20字符，字母数字下划线）',
  `password`        VARCHAR(255) NOT NULL                COMMENT 'BCrypt加密后的密码哈希值',
  `status`          TINYINT(4)   NOT NULL DEFAULT 1      COMMENT '账号状态：1-正常，0-冻结',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最近一次登录时间',
  `is_deleted`      TINYINT(4)   NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号注册时间',
  `updated_at`      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';

-- ------------------------------------------------------------
-- 2. 消费分类表 category
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`         BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `user_id`    BIGINT(20)   NOT NULL DEFAULT 0       COMMENT '所属用户ID：0-系统内置分类，>0-用户自定义分类',
  `name`       VARCHAR(64)  NOT NULL                 COMMENT '分类名称',
  `icon`       VARCHAR(128) DEFAULT NULL             COMMENT '分类图标标识/图标名称（前端映射）',
  `type`       VARCHAR(16)  NOT NULL                 COMMENT '分类类型：income-收入分类，expense-支出分类',
  `sort_order` INT(11)      NOT NULL DEFAULT 0       COMMENT '排序序号（升序排列，数字越小越靠前）',
  `is_deleted` TINYINT(4)   NOT NULL DEFAULT 0       COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name_type` (`user_id`,`name`,`type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费分类表';

-- 系统内置分类种子数据
INSERT INTO `category` (`id`, `user_id`, `name`, `icon`, `type`, `sort_order`) VALUES
(1,  0, '餐饮',   'icon-food',           'expense', 1),
(2,  0, '交通',   'icon-transport',      'expense', 2),
(3,  0, '购物',   'icon-shopping',       'expense', 3),
(4,  0, '娱乐',   'icon-entertainment',  'expense', 4),
(5,  0, '住房',   'icon-housing',        'expense', 5),
(6,  0, '医疗',   'icon-medical',        'expense', 6),
(7,  0, '教育',   'icon-education',      'expense', 7),
(8,  0, '通讯',   'icon-communication',  'expense', 8),
(9,  0, '服饰',   'icon-clothing',       'expense', 9),
(10, 0, '日用品', 'icon-daily',          'expense', 10),
(11, 0, '丽人',   'icon-beauty',         'expense', 11),
(12, 0, '运动',   'icon-sports',         'expense', 12),
(13, 0, '旅行',   'icon-travel',         'expense', 13),
(14, 0, '宠物',   'icon-pet',            'expense', 14),
(15, 0, '数码',   'icon-digital',        'expense', 15),
(16, 0, '其他支出','icon-other-expense', 'expense', 99),
(17, 0, '工资',   'icon-salary',         'income',  1),
(18, 0, '奖金',   'icon-bonus',          'income',  2),
(19, 0, '投资收益','icon-investment',     'income',  3),
(20, 0, '兼职',   'icon-parttime',       'income',  4),
(21, 0, '红包',   'icon-redpacket',      'income',  5),
(22, 0, '退款',   'icon-refund',         'income',  6),
(23, 0, '其他收入','icon-other-income',  'income',  99);

-- ------------------------------------------------------------
-- 3. 账单表 bill
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `bill`;
CREATE TABLE `bill` (
  `id`             BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '账单主键ID',
  `user_id`        BIGINT(20)     NOT NULL                COMMENT '所属用户ID，外键关联sys_user.id',
  `amount`         DECIMAL(12,2)  NOT NULL                COMMENT '金额（正数，收支类型由type字段区分）',
  `type`           VARCHAR(16)    NOT NULL                COMMENT '收支类型：income-收入，expense-支出',
  `category_id`    BIGINT(20)     NOT NULL                COMMENT '消费分类ID，外键关联category.id',
  `remark`         VARCHAR(500)   DEFAULT NULL            COMMENT '文字备注（最大500字符）',
  `receipt_image`  VARCHAR(500)   DEFAULT NULL            COMMENT '小票图片服务端相对路径（如/uploads/receipts/xxx.jpg）',
  `sync_uuid`      VARCHAR(36)    DEFAULT NULL            COMMENT '离线同步UUID v4，用于去重；在线创建为NULL',
  `consume_time`   DATETIME       NOT NULL                COMMENT '消费/收入实际发生时间',
  `is_deleted`     TINYINT(4)     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账单记录创建时间',
  `updated_at`     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '账单最近修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_uuid` (`sync_uuid`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_type` (`type`),
  KEY `idx_consume_time` (`consume_time`),
  KEY `idx_user_consume` (`user_id`,`consume_time`),
  KEY `idx_user_type_time` (`user_id`,`type`,`consume_time`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_bill_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_bill_user`     FOREIGN KEY (`user_id`)     REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单表';

-- ------------------------------------------------------------
-- 4. 预算目标表 budget
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `budget`;
CREATE TABLE `budget` (
  `id`               BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '预算主键ID',
  `user_id`          BIGINT(20)     NOT NULL                COMMENT '所属用户ID，外键关联sys_user.id',
  `yearmonth`        VARCHAR(7)     NOT NULL                COMMENT '预算所属月份，格式YYYY-MM（如2026-06）',
  `total_budget`     DECIMAL(12,2)  NOT NULL DEFAULT 0.00   COMMENT '月度总预算金额',
  `category_budgets` JSON           DEFAULT NULL            COMMENT '各分类子预算JSON：{"category_id": 金额, ...}',
  `is_deleted`       TINYINT(4)     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_yearmonth` (`user_id`,`yearmonth`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_budget_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算目标表';

-- ------------------------------------------------------------
-- 5. 存钱目标表 save_target
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `save_target`;
CREATE TABLE `save_target` (
  `id`            BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '目标主键ID',
  `user_id`       BIGINT(20)     NOT NULL                COMMENT '所属用户ID，外键关联sys_user.id',
  `name`          VARCHAR(128)   NOT NULL                COMMENT '目标名称（如"买MacBook Pro"）',
  `target_amount` DECIMAL(12,2)  NOT NULL                COMMENT '目标总金额',
  `saved_amount`  DECIMAL(12,2)  NOT NULL DEFAULT 0.00   COMMENT '当前已存金额',
  `status`        TINYINT(4)     NOT NULL DEFAULT 0      COMMENT '完成状态：0-进行中，1-已完成',
  `completed_at`  DATETIME       DEFAULT NULL            COMMENT '目标达成时间（status变为1时自动记录）',
  `is_deleted`    TINYINT(4)     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '目标创建时间',
  `updated_at`    DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_target_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存钱目标表';

-- ------------------------------------------------------------
-- 6. 管理员表 sys_admin
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin` (
  `id`              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '管理员主键ID',
  `username`        VARCHAR(64)  NOT NULL                COMMENT '管理员登录账号',
  `password`        VARCHAR(255) NOT NULL                COMMENT 'BCrypt加密后的密码哈希值',
  `role`            VARCHAR(32)  NOT NULL DEFAULT 'OPERATOR' COMMENT '角色权限：SUPER_ADMIN-超级管理员，OPERATOR-运营管理员',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最近一次登录时间',
  `is_deleted`      TINYINT(4)   NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
  `updated_at`      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 默认超管账号: admin / admin123
INSERT INTO `sys_admin` (`id`, `username`, `password`, `role`) VALUES
(1, 'admin', '$2b$10$mabhj31G4lzA1rTytYqV3erPLZdTTCekr9ZkBHeQMQAFBb1LOVWeG', 'SUPER_ADMIN');

-- ------------------------------------------------------------
-- 7. 管理员操作日志表 admin_operation_log
--    v2.0: 新增 admin_role（操作人角色）、resource_id（关联资源ID）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_operation_log`;
CREATE TABLE `admin_operation_log` (
  `id`             BIGINT(20)     NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  `admin_id`       BIGINT(20)     NOT NULL                COMMENT '操作管理员ID',
  `admin_username` VARCHAR(64)    NOT NULL                COMMENT '操作管理员账号（冗余字段，提高查询效率）',
  `admin_role`     VARCHAR(32)    DEFAULT NULL            COMMENT '操作管理员角色：SUPER_ADMIN / OPERATOR',
  `operation`      VARCHAR(128)   NOT NULL                COMMENT '操作类型描述（如"删除账单"、"冻结用户"、"修改分类"）',
  `method`         VARCHAR(16)    NOT NULL                COMMENT 'HTTP请求方法：POST/PUT/DELETE',
  `request_url`    VARCHAR(255)   NOT NULL                COMMENT '请求接口路径（如/api/admin/bill/123）',
  `resource_id`    VARCHAR(64)    DEFAULT NULL            COMMENT '关联资源ID（如userId:123、billId:456），便于追踪"谁动了什么数据"',
  `request_params` TEXT           DEFAULT NULL            COMMENT '请求参数（JSON序列化，敏感字段已脱敏）',
  `ip_address`     VARCHAR(64)    DEFAULT NULL            COMMENT '操作者IP地址',
  `status`         TINYINT(4)     NOT NULL DEFAULT 1      COMMENT '操作结果：1-成功，0-失败',
  `error_msg`      VARCHAR(1000)  DEFAULT NULL            COMMENT '操作失败时的错误信息',
  `is_deleted`     TINYINT(4)     NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `updated_at`     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_operation` (`operation`),
  KEY `idx_admin_time` (`admin_id`,`created_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- ------------------------------------------------------------
-- 8. AI配置表 ai_config
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config` (
  `id`           BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '配置主键ID',
  `config_key`   VARCHAR(128)  NOT NULL                COMMENT '配置键名（如deepseek_api_key、prompt_template_analysis）',
  `config_value` TEXT          NOT NULL                COMMENT '配置值（文本内容、数字参数、JSON等）',
  `config_type`  VARCHAR(32)   NOT NULL DEFAULT 'STRING' COMMENT '配置值类型：STRING-字符串，NUMBER-数字，JSON-JSON，TEXT-长文本',
  `description`  VARCHAR(256)  DEFAULT NULL            COMMENT '配置项说明描述',
  `is_deleted`   TINYINT(4)    NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配置表';

-- AI配置默认种子数据
INSERT INTO `ai_config` (`id`, `config_key`, `config_value`, `config_type`, `description`) VALUES
(1,  'deepseek_api_key',         '${DEEPSEEK_API_KEY:}',                              'STRING', 'DeepSeek官方API密钥（通过环境变量DEEPSEEK_API_KEY配置）'),
(2,  'model_base_url',           'https://api.deepseek.com',                          'STRING', 'DeepSeek官方API地址（OpenAI兼容接口）'),
(3,  'model_name',               'deepseek-v4-flash',                                 'STRING', '大模型名称'),
(4,  'model_temperature',        '0.7',                                                'NUMBER', '模型temperature参数（0-2），控制输出随机性'),
(5,  'model_max_tokens',         '4096',                                               'NUMBER', '模型max_tokens参数，最大输出长度'),
(6,  'model_top_p',              '0.7',                                                'NUMBER', '模型top_p核采样参数'),
(7,  'dashscope_api_key',        '${DASHSCOPE_API_KEY:sk-your-dashscope-api-key}',     'STRING', '阿里云百炼DashScope API密钥'),
(8,  'embedding_model',          'text-embedding-v2',                                  'STRING', '阿里云百炼文本嵌入模型名称'),
(9,  'embedding_dimension',      '1536',                                               'NUMBER', '向量维度（text-embedding-v2 = 1536）'),
(10, 'qdrant_host',              'localhost',                                          'STRING', 'Qdrant向量数据库服务地址'),
(11, 'qdrant_port',              '6334',                                               'NUMBER', 'Qdrant gRPC通信端口'),
(12, 'qdrant_collection',        'user_consumption_vectors',                           'STRING', 'Qdrant向量集合名称'),
(13, 'prompt_template_analysis', '你是一名专业的个人理财顾问，持有CFP认证。\n请基于以下用户月度账单数据进行全面的财务诊断分析。\n\n{{markdown_bill_data}}\n\n请严格按以下JSON格式返回（不要包含```json```代码块）：\n{\n  "overview": {"totalIncome": 0, "totalExpense": 0, "balance": 0, "healthScore": 0, "summary": ""},\n  "wasteItems": [{"name": "", "amount": 0, "category": "", "reason": "", "suggestion": "", "severity": "MEDIUM"}],\n  "badHabits": [{"habit": "", "description": "", "impact": "", "severity": "MEDIUM"}],\n  "suggestions": [{"plan": "", "description": "", "estimatedMonthlySave": "", "difficulty": "MODERATE"}]\n}', 'TEXT', 'AI分析Prompt模板'),
(14, 'prompt_template_chat',     '你是一名专业的个人理财顾问，用户正在咨询个人财务管理问题。请基于用户提供的账单数据，以专业、友好、易懂的方式回答。', 'TEXT', 'AI对话Prompt模板'),
(15, 'prompt_template_export',   '请基于以下账单数据生成一份专业的月度财务报告，包含收支总览、分类分析、消费建议。', 'TEXT', 'AI导出报告Prompt模板');

-- ------------------------------------------------------------
-- 9. AI分析记录表 ai_analysis_record
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_analysis_record`;
CREATE TABLE `ai_analysis_record` (
  `id`                      BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '分析记录主键ID',
  `user_id`                 BIGINT(20)   NOT NULL                COMMENT '所属用户ID，外键关联sys_user.id',
  `yearmonth`               VARCHAR(7)   NOT NULL                COMMENT '分析账单所属月份，格式YYYY-MM',
  `result_json`             MEDIUMTEXT   NOT NULL                COMMENT 'AI分析结果JSON（含overview/wasteItems/badHabits/suggestions/nextMonthPlan）',
  `prompt_template_snapshot` TEXT         DEFAULT NULL            COMMENT '本次分析使用的Prompt模板文本快照（用于回溯）',
  `model_name`              VARCHAR(64)  NOT NULL DEFAULT 'DeepSeek' COMMENT '调用的大模型名称',
  `processing_time_ms`      BIGINT(20)   DEFAULT NULL            COMMENT 'AI处理耗时（毫秒），用于性能监控',
  `is_deleted`              TINYINT(4)   NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析完成时间',
  `updated_at`              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_yearmonth` (`yearmonth`),
  KEY `idx_user_month` (`user_id`,`yearmonth`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析记录表';

-- ------------------------------------------------------------
-- 10. 数据库备份日志表 database_backup_log
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `database_backup_log`;
CREATE TABLE `database_backup_log` (
  `id`         BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '备份日志主键ID',
  `admin_id`   BIGINT(20)    NOT NULL                COMMENT '执行备份的管理员ID',
  `file_name`  VARCHAR(255)  NOT NULL                COMMENT '备份文件名（如backup_20260623_143000.sql）',
  `file_path`  VARCHAR(500)  NOT NULL                COMMENT '备份文件完整路径',
  `file_size`  BIGINT(20)    DEFAULT NULL            COMMENT '备份文件大小（字节）',
  `status`     TINYINT(4)    NOT NULL DEFAULT 1      COMMENT '备份结果：1-成功，0-失败',
  `error_msg`  VARCHAR(1000) DEFAULT NULL            COMMENT '备份失败时的错误信息',
  `is_deleted` TINYINT(4)    NOT NULL DEFAULT 0      COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份执行时间',
  `updated_at` DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库备份日志表';