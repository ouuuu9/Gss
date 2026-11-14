-- ======================================================================
-- 办单审核闭环迁移脚本（M1，2026-11-14）
-- 适用：已按旧版 gss.sql 建库的环境；新库直接用 gss.sql 即可。
-- 特性：可重复执行（每步先查 information_schema 再改），末尾附回滚段。
-- 依赖：MySQL 5.7+。执行前请备份。
-- 语义约定：review_* = 办单审核；audit_time = 闸口审单，两套语义不混用。
-- ======================================================================

-- 0. 幂等加列：办单申请 / 审核字段
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN apply_by BIGINT DEFAULT NULL COMMENT ''办单申请人用户ID'' AFTER remark',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'apply_by');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN apply_time DATETIME DEFAULT NULL COMMENT ''办单申请时间（重新提交时刷新）'' AFTER apply_by',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'apply_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''办单审核状态：PENDING待审核/APPROVED已通过/REJECTED已驳回（历史数据视为APPROVED）'' AFTER apply_time',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'review_status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN review_by BIGINT DEFAULT NULL COMMENT ''办单审核人用户ID'' AFTER review_status',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'review_by');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN review_time DATETIME DEFAULT NULL COMMENT ''办单审核时间（与闸口审单 audit_time 语义不同）'' AFTER review_by',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'review_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD COLUMN reject_reason VARCHAR(200) DEFAULT NULL COMMENT ''驳回原因（驳回必填，重新提交时清空）'' AFTER review_time',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND COLUMN_NAME = 'reject_reason');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1. 幂等加索引：审核状态查询（审核待办列表）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE eir_order ADD INDEX idx_review_status (review_status)',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eir_order' AND INDEX_NAME = 'idx_review_status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 历史数据口径：CREATED 视为 APPROVED（保证旧数据仍可派单/演示）
UPDATE eir_order
SET status = 'APPROVED',
    review_status = 'APPROVED',
    review_time = COALESCE(review_time, create_time)
WHERE status = 'CREATED';

-- 3. 其余历史单证补审核留痕（已派单/已预约/已进闸/已出闸/已完成均代表审核已通过）
UPDATE eir_order
SET review_status = 'APPROVED',
    review_time = COALESCE(review_time, create_time)
WHERE status IN ('APPROVED', 'DISPATCHED', 'APPOINTED', 'GATE_IN', 'GATE_OUT', 'DONE')
  AND (review_status IS NULL OR review_status <> 'APPROVED');

-- 4. 校验（人工核对）：
-- SELECT status, review_status, COUNT(*) FROM eir_order GROUP BY status, review_status;

-- ======================================================================
-- 回滚段（谨慎执行；注意第 3 条会把新流程中已审核的单证一并回退为 CREATED）
-- ======================================================================
-- ALTER TABLE eir_order DROP INDEX idx_review_status;
-- ALTER TABLE eir_order
--     DROP COLUMN reject_reason,
--     DROP COLUMN review_time,
--     DROP COLUMN review_by,
--     DROP COLUMN review_status,
--     DROP COLUMN apply_time,
--     DROP COLUMN apply_by;
-- UPDATE eir_order SET status = 'CREATED' WHERE status IN ('APPROVED', 'SUBMITTED', 'REJECTED');