-- ======================================================================
-- M3 闸口车道维度配额  迁移脚本（2026-11-15）
-- 适用：已按旧版 gss.sql 建库的环境（含 M1/M2 迁移）；新库直接用 gss.sql。
-- 特性：可重复执行；唯一键由 (appt_date, time_slot) 改为 (lane_id, appt_date, time_slot)。
-- 顺序不可调换：建表 → 默认车道 → 加列 → 回填 → 改唯一键 → NOT NULL。
-- ======================================================================

-- 1. 车道表
CREATE TABLE IF NOT EXISTS gate_lane (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '车道ID',
    gate_no           VARCHAR(20) NOT NULL COMMENT '闸口号，如 GATE-1',
    lane_no           VARCHAR(20) NOT NULL COMMENT '车道号，如 LANE-1',
    capacity_per_slot INT         NOT NULL DEFAULT 10 COMMENT '单时段默认容量',
    status            VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用/DISABLED停用',
    remark            VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gate_lane (gate_no, lane_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闸口车道表';

-- 2. 默认车道（容量取默认值 10；已有的不动）
INSERT INTO gate_lane (gate_no, lane_no, capacity_per_slot, status, remark)
SELECT 'GATE-1', 'LANE-1', 10, 'ENABLED', '默认车道（M3 迁移基线）'
WHERE NOT EXISTS (SELECT 1 FROM gate_lane WHERE gate_no = 'GATE-1' AND lane_no = 'LANE-1');

SET @default_lane := (SELECT id FROM gate_lane WHERE gate_no = 'GATE-1' AND lane_no = 'LANE-1' LIMIT 1);

-- 3. appointment 加 lane_id（先可空，回填后转 NOT NULL 由应用保证；历史行必须回填）
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appointment ADD COLUMN lane_id BIGINT DEFAULT NULL COMMENT ''预约车道ID（M3）'' AFTER driver_id',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'lane_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. appt_slot_capacity 加 lane_id
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appt_slot_capacity ADD COLUMN lane_id BIGINT DEFAULT NULL COMMENT ''闸口车道ID（M3）'' AFTER id',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appt_slot_capacity' AND COLUMN_NAME = 'lane_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 容量记录挂到默认车道（capacity 保持原值，保证总容量不缩水）
UPDATE appt_slot_capacity SET lane_id = @default_lane WHERE lane_id IS NULL;

-- 6. 历史预约挂到默认车道
UPDATE appointment SET lane_id = @default_lane WHERE lane_id IS NULL;

-- 7. 先删旧唯一键，再建新唯一键（顺序不能反，否则 (lane,date,slot) 插入会被旧键挡住）
SET @sql = (SELECT IF(COUNT(*) > 0,
    'ALTER TABLE appt_slot_capacity DROP INDEX uk_date_slot',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appt_slot_capacity' AND INDEX_NAME = 'uk_date_slot');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appt_slot_capacity ADD UNIQUE KEY uk_lane_date_slot (lane_id, appt_date, time_slot)',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appt_slot_capacity' AND INDEX_NAME = 'uk_lane_date_slot');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE appt_slot_capacity ADD KEY idx_date_slot (appt_date, time_slot)',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appt_slot_capacity' AND INDEX_NAME = 'idx_date_slot');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8. lane_id 收口为 NOT NULL（不允许用可空规避迁移）
SET @sql = (SELECT IF(COUNT(*) > 0,
    'ALTER TABLE appt_slot_capacity MODIFY COLUMN lane_id BIGINT NOT NULL COMMENT ''闸口车道ID（M3）''',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appt_slot_capacity' AND COLUMN_NAME = 'lane_id' AND IS_NULLABLE = 'YES');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) > 0,
    'ALTER TABLE appointment MODIFY COLUMN lane_id BIGINT NOT NULL COMMENT ''预约车道ID（M3）''',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'lane_id' AND IS_NULLABLE = 'YES');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 9. 容量核对（迁移前后 SUM(capacity) 应一致）
-- SELECT SUM(capacity) AS total_capacity, COUNT(*) AS rows_cnt FROM appt_slot_capacity;

-- ======================================================================
-- 回滚段（谨慎执行；若已产生多车道同日期同时段容量，还原旧唯一键会失败，
-- 需要先删掉非默认车道的数据，这是不可逆的业务取舍，脚本不替你做决定）
-- ======================================================================
-- ALTER TABLE appt_slot_capacity DROP INDEX uk_lane_date_slot;
-- ALTER TABLE appt_slot_capacity DROP INDEX idx_date_slot;
-- ALTER TABLE appt_slot_capacity ADD UNIQUE KEY uk_date_slot (appt_date, time_slot);
-- ALTER TABLE appt_slot_capacity DROP COLUMN lane_id;
-- ALTER TABLE appointment DROP COLUMN lane_id;
-- DROP TABLE IF EXISTS gate_lane;