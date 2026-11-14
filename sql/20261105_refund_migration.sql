-- 退款闭环迁移脚本（2026-11-05）
-- 适用：已按旧版 gss.sql 建库的环境；新库直接用 gss.sql 即可。
-- 说明：MySQL 5.7+（生成列 + 唯一索引）。执行前请备份。

-- 1. 收费记录增加退款字段，缴费状态扩展 2=已退
ALTER TABLE fee_record
    MODIFY COLUMN pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '缴费状态：0未缴 1已缴 2已退',
    ADD COLUMN refund_time DATETIME DEFAULT NULL COMMENT '退款时间' AFTER operator,
    ADD COLUMN refund_operator VARCHAR(50) DEFAULT NULL COMMENT '退款操作人' AFTER refund_time;

-- 2. 退款记录表
CREATE TABLE refund_record (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '退款记录ID',
    refund_no       VARCHAR(32)   DEFAULT NULL COMMENT '退款编号（审批通过时生成）',
    fee_record_id   BIGINT        NOT NULL COMMENT '关联收费记录ID',
    eir_id          BIGINT        NOT NULL COMMENT '关联单证ID',
    company_id      BIGINT        NOT NULL COMMENT '缴费公司ID',
    amount          DECIMAL(10,2) NOT NULL COMMENT '退款金额（按单证整笔退）',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审批/APPROVED已退款/REJECTED已驳回',
    applicant       VARCHAR(50)   NOT NULL COMMENT '申请人',
    applied_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approver        VARCHAR(50)   DEFAULT NULL COMMENT '审批人',
    approved_at     DATETIME      DEFAULT NULL COMMENT '审批/处理时间',
    reject_reason   VARCHAR(255)  DEFAULT NULL COMMENT '驳回原因',
    remark          VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    pending_eir_id  BIGINT GENERATED ALWAYS AS (IF(status = 'PENDING', eir_id, NULL)) STORED COMMENT '待审批唯一键（非 PENDING 为 NULL）',
    refunded_fee_id BIGINT GENERATED ALWAYS AS (IF(status = 'APPROVED', fee_record_id, NULL)) STORED COMMENT '已退款唯一键（非 APPROVED 为 NULL）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_pending_eir (pending_eir_id),
    UNIQUE KEY uk_refunded_fee (refunded_fee_id),
    KEY idx_fee (fee_record_id),
    KEY idx_eir (eir_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- 回滚（谨慎）：
-- DROP TABLE refund_record;
-- ALTER TABLE fee_record DROP COLUMN refund_operator, DROP COLUMN refund_time,
--     MODIFY COLUMN pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '缴费状态：0未缴 1已缴';
