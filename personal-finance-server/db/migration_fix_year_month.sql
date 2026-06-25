-- ============================================================
-- 数据库迁移脚本：修复 year_month 保留字冲突
-- 错误：BadSqlGrammarException - 'year_month' is reserved keyword in MySQL 8
-- 方案：重命名列 year_month → yearmonth（去掉下划线）
-- ============================================================

-- 1. budget 表：重命名列 + 重建唯一索引
ALTER TABLE `budget`
    CHANGE COLUMN `year_month` `yearmonth` VARCHAR(7) NOT NULL
    COMMENT '预算所属月份，格式YYYY-MM（如2026-06）';

ALTER TABLE `budget`
    DROP INDEX `uk_user_year_month`,
    ADD UNIQUE KEY `uk_user_yearmonth` (`user_id`, `yearmonth`);

-- 2. ai_analysis_record 表：重命名列 + 重建索引
ALTER TABLE `ai_analysis_record`
    CHANGE COLUMN `year_month` `yearmonth` VARCHAR(7) NOT NULL
    COMMENT '分析账单所属月份，格式YYYY-MM';

ALTER TABLE `ai_analysis_record`
    DROP INDEX `idx_year_month`,
    ADD KEY `idx_yearmonth` (`yearmonth`);

ALTER TABLE `ai_analysis_record`
    DROP INDEX `idx_user_month`,
    ADD KEY `idx_user_month` (`user_id`, `yearmonth`);

-- 验证
SELECT '迁移完成：year_month → yearmonth' AS result;
SELECT table_name, column_name FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_name IN ('yearmonth', 'year_month')
ORDER BY table_name, column_name;
