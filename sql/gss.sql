-- ======================================================================
-- 集装箱码头闸口预约与办单管理系统 建库脚本
-- 数据库：MySQL 8.x，字符集 utf8mb4
-- 说明：系统账号（sys_user）由后端首次启动时自动初始化（BCrypt 加密）
--       默认账号见 backend 的 DataInitializer，密码均为 123456
-- ======================================================================

DROP DATABASE IF EXISTS gss;
CREATE DATABASE gss DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE gss;

-- ----------------------------------------------------------------------
-- 1. 运输公司表
-- ----------------------------------------------------------------------
CREATE TABLE transport_company (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公司ID',
    code        VARCHAR(20)  NOT NULL COMMENT '公司编码（唯一）',
    name        VARCHAR(100) NOT NULL COMMENT '公司名称',
    contact     VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输公司表';

-- ----------------------------------------------------------------------
-- 2. 系统用户表
-- ----------------------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名（唯一）',
    password    VARCHAR(100) NOT NULL COMMENT '登录密码（BCrypt加密）',
    name        VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL COMMENT '角色：ADMIN码头管理员/GATE闸口办单员/COMPANY运输公司用户/DRIVER司机',
    company_id  BIGINT       DEFAULT NULL COMMENT '所属运输公司ID（码头端用户为空）',
    driver_id   BIGINT       DEFAULT NULL COMMENT '关联司机ID（仅DRIVER角色，用于数据权限过滤）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------------------------------------------------
-- 3. 拖车表
-- ----------------------------------------------------------------------
CREATE TABLE tractor (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '拖车ID',
    plate_no    VARCHAR(20)  NOT NULL COMMENT '拖头车牌号',
    frame_no    VARCHAR(50)  DEFAULT NULL COMMENT '车架号',
    ic_card_no  VARCHAR(30)  NOT NULL COMMENT 'IC卡编号（模拟刷卡，唯一）',
    company_id  BIGINT       NOT NULL COMMENT '所属公司ID',
    axle_num    INT          NOT NULL DEFAULT 4 COMMENT '轴数',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ic_card_no (ic_card_no),
    KEY idx_company (company_id),
    KEY idx_plate (plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拖车表';

-- ----------------------------------------------------------------------
-- 4. 司机表
-- ----------------------------------------------------------------------
CREATE TABLE driver (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '司机ID',
    name        VARCHAR(50)  NOT NULL COMMENT '司机姓名',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号（司机账号用户名与手机号一致）',
    license_no  VARCHAR(50)  DEFAULT NULL COMMENT '驾驶证号',
    tractor_id  BIGINT       DEFAULT NULL COMMENT '绑定拖车ID',
    company_id  BIGINT       NOT NULL COMMENT '所属公司ID',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_company (company_id),
    KEY idx_tractor (tractor_id),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机表';

-- ----------------------------------------------------------------------
-- 5. EIR单证表
-- ----------------------------------------------------------------------
CREATE TABLE eir_order (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '单证ID',
    eir_no         VARCHAR(50)  NOT NULL COMMENT '单证编号（唯一）',
    business_type  VARCHAR(10)  NOT NULL COMMENT '业务类型：PI-F提重/PI-E提空/GR-E还空/GR-F还重',
    container_no   VARCHAR(30)  NOT NULL COMMENT '集装箱号',
    container_size INT          NOT NULL COMMENT '箱尺寸：20/40',
    container_type VARCHAR(10)  NOT NULL COMMENT '箱型：GP/HQ',
    company_id     BIGINT       NOT NULL COMMENT '作业公司ID',
    tractor_id     BIGINT       DEFAULT NULL COMMENT '派单拖车ID',
    driver_id      BIGINT       DEFAULT NULL COMMENT '派单司机ID',
    status         VARCHAR(20)  NOT NULL DEFAULT 'CREATED'
                   COMMENT '单证状态：CREATED已录入/DISPATCHED已派单/APPOINTED已预约/GATE_IN已进闸/GATE_OUT已出闸/DONE已完成/CANCELLED已作废',
    gate_in_time   DATETIME     DEFAULT NULL COMMENT '进闸时间',
    gate_out_time  DATETIME     DEFAULT NULL COMMENT '出闸时间',
    remark         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_eir_no (eir_no),
    KEY idx_company (company_id),
    KEY idx_status (status),
    KEY idx_tractor (tractor_id),
    KEY idx_driver (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EIR单证表';

-- ----------------------------------------------------------------------
-- 6. 进港预约表
-- ----------------------------------------------------------------------
CREATE TABLE appointment (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    eir_id      BIGINT      NOT NULL COMMENT '关联单证ID',
    plate_no    VARCHAR(20) NOT NULL COMMENT '预约车牌号',
    appt_date   DATE        NOT NULL COMMENT '预约日期',
    time_slot   VARCHAR(20) DEFAULT NULL COMMENT '预约时段（8个两小时时段，如 08:00-10:00）',
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE有效/CANCELLED已取消',
    driver_id   BIGINT      NOT NULL COMMENT '司机ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_eir (eir_id),
    KEY idx_driver (driver_id),
    KEY idx_appt_date (appt_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进港预约表';

-- ----------------------------------------------------------------------
-- 7. 收费记录表
-- ----------------------------------------------------------------------
CREATE TABLE fee_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '收费记录ID',
    eir_id      BIGINT        NOT NULL COMMENT '关联单证ID',
    plate_no    VARCHAR(20)   DEFAULT NULL COMMENT '车牌号',
    company_id  BIGINT        NOT NULL COMMENT '缴费公司ID',
    amount      DECIMAL(10,2) NOT NULL COMMENT '应收金额（固定50元/单，仅GR-F还重单生成）',
    pay_status  TINYINT       NOT NULL DEFAULT 0 COMMENT '缴费状态：0未缴 1已缴',
    pay_time    DATETIME      DEFAULT NULL COMMENT '缴费时间',
    operator    VARCHAR(50)   DEFAULT NULL COMMENT '操作人',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_eir (eir_id),
    KEY idx_company (company_id),
    KEY idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收费记录表';

-- ----------------------------------------------------------------------
-- 8. 预约时段容量表（每时段最大预约数，创建预约时 CAS 防超约）
-- ----------------------------------------------------------------------
CREATE TABLE appt_slot_capacity (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '容量ID',
    appt_date    DATE        NOT NULL COMMENT '预约日期',
    time_slot    VARCHAR(20) NOT NULL COMMENT '预约时段（如 08:00-10:00）',
    capacity     INT         NOT NULL DEFAULT 10 COMMENT '时段最大预约数',
    booked_count INT         NOT NULL DEFAULT 0 COMMENT '已预约数',
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_slot (appt_date, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约时段容量表';

-- ----------------------------------------------------------------------
-- 9. 操作审计日志表（AOP 自动记录，谁/何时/从哪个IP/干了什么/结果）
-- ----------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人用户名',
    role        VARCHAR(20)  DEFAULT NULL COMMENT '操作人角色',
    operation   VARCHAR(100) DEFAULT NULL COMMENT '操作描述（注解value）',
    method      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
    uri         VARCHAR(200) DEFAULT NULL COMMENT '请求URI',
    params      TEXT         COMMENT '请求参数（JSON，截断至500字符）',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '客户端IP',
    success     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
    error_msg   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    cost_ms     INT          DEFAULT NULL COMMENT '耗时（毫秒）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ======================================================================
-- 种子数据（均为通用虚构命名）
-- ======================================================================

-- 运输公司
INSERT INTO transport_company (id, code, name, contact, phone) VALUES
(1, 'COMP001', '第一运输有限公司', '王经理', '13900000001'),
(2, 'COMP002', '第二运输有限公司', '赵经理', '13900000002');

-- 拖车（IC卡为模拟刷卡编号）
INSERT INTO tractor (id, plate_no, frame_no, ic_card_no, company_id, axle_num) VALUES
(1, '粤A10001', 'FRAME0000000001', 'IC0001', 1, 4),
(2, '粤A10002', 'FRAME0000000002', 'IC0002', 1, 4),
(3, '粤A20001', 'FRAME0000000003', 'IC0003', 2, 6);

-- 司机（司机登录账号用户名 = 手机号）
INSERT INTO driver (id, name, phone, license_no, tractor_id, company_id) VALUES
(1, '张伟', '13800000001', 'LIC00000001', 1, 1),
(2, '陈强', '13800000002', 'LIC00000002', 2, 1),
(3, '李明', '13800000003', 'LIC00000003', 3, 2);

-- 示例单证：一张已录入状态，方便直接体验派单流程
INSERT INTO eir_order (id, eir_no, business_type, container_no, container_size, container_type, company_id, remark)
VALUES (1, 'EIR20261011001', 'GR-F', 'CSNU1000001', 40, 'HQ', 1, '初始化示例单证（还重，需缴费后进闸）');

INSERT INTO eir_order (id, eir_no, business_type, container_no, container_size, container_type, company_id, remark)
VALUES (2, 'EIR20261011002', 'PI-E', 'CSNU1000002', 20, 'GP', 2, '初始化示例单证（提空，无需缴费）');
