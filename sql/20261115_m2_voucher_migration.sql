-- ======================================================================
-- M2 预约凭证 + 闸口核验留痕 + 失约口径修正  迁移脚本（2026-11-15）
-- 适用：已按旧版 gss.sql 建库的环境；新库直接用 gss.sql 即可。
-- 特性：可重复执行（逐列/逐索引查 information_schema），末尾附回滚段。
-- 语义：review_* = 办单审核；audit_time = 闸口审单；verify_time/release_time = 预约核验/放行。
-- ======================================================================

-- 0. 幂等加列
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appointment ADD COLUMN voucher_no VARCHAR(24) DEFAULT NULL COMMENT ''预约凭证号（APT+yyyyMMdd+6位当日序列，唯一）'' AFTER driver_id',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'voucher_no');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appointment ADD COLUMN verify_time DATETIME DEFAULT NULL COMMENT ''闸口核验时间（为空表示未核验，失约判定依赖它）'' AFTER voucher_no',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'verify_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appointment ADD COLUMN release_time DATETIME DEFAULT NULL COMMENT ''放行时间（出闸释放时写入）'' AFTER verify_time',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'release_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1. 幂等加唯一索引（并发凭证号兜底）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appointment ADD UNIQUE KEY uk_voucher_no (voucher_no)',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND INDEX_NAME = 'uk_voucher_no');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 历史预约补凭证号：APT + 创建日期 + 当日序号（按 id 排序，6 位补零）
UPDATE appointment a
JOIN (
    SELECT id,
           CONCAT('APT', DATE_FORMAT(create_time, '%Y%m%d'),
                  LPAD(ROW_NUMBER() OVER (PARTITION BY DATE(create_time) ORDER BY id), 6, '0')) AS vno
    FROM appointment
    WHERE voucher_no IS NULL
) t ON t.id = a.id
SET a.voucher_no = t.vno
WHERE a.voucher_no IS NULL;

-- 3. 失约口径：旧 EXPIRED → NO_SHOW
UPDATE appointment SET status = 'NO_SHOW' WHERE status = 'EXPIRED';

-- 4. 幂等建核验留痕表
CREATE TABLE IF NOT EXISTS gate_check_log (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    voucher_no     VARCHAR(24) DEFAULT NULL COMMENT '凭证号（IC 卡核验时回填对应预约凭证号）',
    eir_id         BIGINT      DEFAULT NULL COMMENT '关联单证ID（查不到预约时为空）',
    appointment_id BIGINT      DEFAULT NULL COMMENT '关联预约ID（查不到预约时为空）',
    check_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核验时间',
    result         VARCHAR(10) NOT NULL COMMENT '结果：PASS通过/FAIL失败',
    abnormal_type  VARCHAR(30) DEFAULT NULL COMMENT '异常类型：NO_APPOINTMENT/CANCELLED/EXPIRED/DOC_MISMATCH/EARLY/LATE',
    operator_id    BIGINT      DEFAULT NULL COMMENT '操作人用户ID',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '备注（IC卡、车道号、多预约提示等）',
    PRIMARY KEY (id),
    KEY idx_voucher (voucher_no),
    KEY idx_eir (eir_id),
    KEY idx_check_time (check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闸口核验留痕表';

-- 5. 校验（人工核对）
-- SELECT status, COUNT(*) FROM appointment GROUP BY status;
-- SELECT COUNT(*) AS missing_voucher FROM appointment WHERE voucher_no IS NULL;

-- ======================================================================
-- 回滚段（谨慎执行）
-- ======================================================================
-- DROP TABLE IF EXISTS gate_check_log;
-- UPDATE appointment SET status = 'EXPIRED' WHERE status = 'NO_SHOW';
-- ALTER TABLE appointment DROP INDEX uk_voucher_no;
-- ALTER TABLE appointment
--     DROP COLUMN release_time,
--     DROP COLUMN verify_time,
--     DROP COLUMN voucher_no;