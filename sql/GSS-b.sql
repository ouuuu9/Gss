/*
 Navicat Premium Dump SQL

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : gss

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 17/10/2026 12:02:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for appointment
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `eir_id` bigint NOT NULL COMMENT '关联单证ID',
  `plate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约车牌号',
  `appt_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预约时段（8个两小时时段，如 08:00-10:00）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE有效/CANCELLED已取消',
  `driver_id` bigint NOT NULL COMMENT '司机ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_eir`(`eir_id` ASC) USING BTREE,
  INDEX `idx_driver`(`driver_id` ASC) USING BTREE,
  INDEX `idx_appt_date`(`appt_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '进港预约表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES (1, 40, '粤A10001', '2026-10-13', '20:00-22:00', 'ACTIVE', 1, '2026-10-12 21:20:44', '2026-10-12 21:20:44');
INSERT INTO `appointment` VALUES (2, 38, '粤A10001', '2026-10-13', '06:00-08:00', 'EXPIRED', 1, '2026-10-12 21:20:55', '2026-10-13 16:25:58');
INSERT INTO `appointment` VALUES (3, 36, '粤A10001', '2026-10-13', '12:00-14:00', 'EXPIRED', 1, '2026-10-12 21:21:04', '2026-10-13 16:25:58');
INSERT INTO `appointment` VALUES (4, 29, '粤A10001', '2026-10-13', '14:00-16:00', 'EXPIRED', 1, '2026-10-12 21:50:33', '2026-10-13 16:25:58');
INSERT INTO `appointment` VALUES (5, 34, '粤A10002', '2026-10-13', '10:00-12:00', 'EXPIRED', 2, '2026-10-12 21:56:30', '2026-10-13 16:25:58');
INSERT INTO `appointment` VALUES (6, 32, '粤A10002', '2026-10-13', '08:00-10:00', 'EXPIRED', 2, '2026-10-12 21:56:37', '2026-10-13 16:25:58');
INSERT INTO `appointment` VALUES (7, 20, '粤A10001', '2026-10-13', '10:00-12:00', 'EXPIRED', 1, '2026-10-12 22:25:27', '2026-10-13 16:25:58');

-- ----------------------------
-- Table structure for appt_slot_capacity
-- ----------------------------
DROP TABLE IF EXISTS `appt_slot_capacity`;
CREATE TABLE `appt_slot_capacity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '容量ID',
  `appt_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预约时段（如 08:00-10:00）',
  `capacity` int NOT NULL DEFAULT 10 COMMENT '时段最大预约数',
  `booked_count` int NOT NULL DEFAULT 0 COMMENT '已预约数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_date_slot`(`appt_date` ASC, `time_slot` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约时段容量表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of appt_slot_capacity
-- ----------------------------
INSERT INTO `appt_slot_capacity` VALUES (1, '2026-10-13', '20:00-22:00', 10, 1);
INSERT INTO `appt_slot_capacity` VALUES (2, '2026-10-13', '06:00-08:00', 10, 0);
INSERT INTO `appt_slot_capacity` VALUES (3, '2026-10-13', '12:00-14:00', 10, 0);
INSERT INTO `appt_slot_capacity` VALUES (4, '2026-10-13', '14:00-16:00', 10, 0);
INSERT INTO `appt_slot_capacity` VALUES (5, '2026-10-13', '10:00-12:00', 10, 0);
INSERT INTO `appt_slot_capacity` VALUES (6, '2026-10-13', '08:00-10:00', 10, 0);

-- ----------------------------
-- Table structure for audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人用户名',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人角色',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述（注解value）',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `uri` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URI',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数（JSON，截断至500字符）',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `cost_ms` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 104 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作审计日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_log
-- ----------------------------
INSERT INTO `audit_log` VALUES (1, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:12:28');
INSERT INTO `audit_log` VALUES (2, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 106, '2026-10-12 21:12:31');
INSERT INTO `audit_log` VALUES (3, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:12:48');
INSERT INTO `audit_log` VALUES (4, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 124, '2026-10-12 21:12:50');
INSERT INTO `audit_log` VALUES (5, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:13:00');
INSERT INTO `audit_log` VALUES (6, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 166, '2026-10-12 21:13:02');
INSERT INTO `audit_log` VALUES (7, 'company1', 'COMPANY', '单证派单', 'POST', '/api/eir/40/dispatch', '[40,{\"driverId\":1,\"tractorId\":1}]', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-10-12 21:13:08');
INSERT INTO `audit_log` VALUES (8, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:14:47');
INSERT INTO `audit_log` VALUES (9, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 79, '2026-10-12 21:14:49');
INSERT INTO `audit_log` VALUES (10, '13800000001', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:17:01');
INSERT INTO `audit_log` VALUES (11, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 73, '2026-10-12 21:17:03');
INSERT INTO `audit_log` VALUES (12, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[38,36],\"driverId\":1,\"tractorId\":1}]', '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-10-12 21:17:18');
INSERT INTO `audit_log` VALUES (13, 'company1', 'COMPANY', '单证派单', 'POST', '/api/eir/34/dispatch', '[34,{\"driverId\":2,\"tractorId\":2}]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:17:26');
INSERT INTO `audit_log` VALUES (14, 'company1', 'COMPANY', '单证派单', 'POST', '/api/eir/32/dispatch', '[32,{\"driverId\":2,\"tractorId\":2}]', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-10-12 21:17:45');
INSERT INTO `audit_log` VALUES (15, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[29,27,25],\"driverId\":1,\"tractorId\":1}]', '0:0:0:0:0:0:0:1', 1, NULL, 22, '2026-10-12 21:17:55');
INSERT INTO `audit_log` VALUES (16, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[23,21],\"driverId\":2,\"tractorId\":2}]', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-10-12 21:18:01');
INSERT INTO `audit_log` VALUES (17, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[20,18,16],\"driverId\":1,\"tractorId\":1}]', '0:0:0:0:0:0:0:1', 1, NULL, 22, '2026-10-12 21:18:40');
INSERT INTO `audit_log` VALUES (18, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[14,12],\"driverId\":2,\"tractorId\":2}]', '0:0:0:0:0:0:0:1', 1, NULL, 30, '2026-10-12 21:18:50');
INSERT INTO `audit_log` VALUES (19, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[9,7,5],\"driverId\":1,\"tractorId\":1}]', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-10-12 21:18:59');
INSERT INTO `audit_log` VALUES (20, 'company1', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[1,3],\"driverId\":2,\"tractorId\":2}]', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-10-12 21:19:06');
INSERT INTO `audit_log` VALUES (21, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/10/pay', '[10]', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-10-12 21:19:51');
INSERT INTO `audit_log` VALUES (22, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/6/pay', '[6]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:19:54');
INSERT INTO `audit_log` VALUES (23, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/4/pay', '[4]', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-10-12 21:19:56');
INSERT INTO `audit_log` VALUES (24, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/2/pay', '[2]', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-10-12 21:19:59');
INSERT INTO `audit_log` VALUES (25, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/1/pay', '[1]', '0:0:0:0:0:0:0:1', 1, NULL, 4, '2026-10-12 21:20:02');
INSERT INTO `audit_log` VALUES (26, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/7/pay', '[7]', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-10-12 21:20:04');
INSERT INTO `audit_log` VALUES (27, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:20:14');
INSERT INTO `audit_log` VALUES (28, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 66, '2026-10-12 21:20:17');
INSERT INTO `audit_log` VALUES (29, '13800000001', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":40,\"plateNo\":\"粤A10001\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"20:00-22:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 15, '2026-10-12 21:20:44');
INSERT INTO `audit_log` VALUES (30, '13800000001', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":38,\"plateNo\":\"粤A10001\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"06:00-08:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:20:55');
INSERT INTO `audit_log` VALUES (31, '13800000001', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":36,\"plateNo\":\"粤A10001\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"12:00-14:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-10-12 21:21:04');
INSERT INTO `audit_log` VALUES (32, '13800000001', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:49:36');
INSERT INTO `audit_log` VALUES (33, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 65, '2026-10-12 21:49:38');
INSERT INTO `audit_log` VALUES (34, '13800000001', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":29,\"plateNo\":\"粤A10001\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"14:00-16:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-10-12 21:50:33');
INSERT INTO `audit_log` VALUES (35, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 68, '2026-10-12 21:51:55');
INSERT INTO `audit_log` VALUES (36, '13800000001', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:55:05');
INSERT INTO `audit_log` VALUES (37, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000002\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 70, '2026-10-12 21:55:11');
INSERT INTO `audit_log` VALUES (38, '13800000002', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":34,\"plateNo\":\"粤A10002\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"10:00-12:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-10-12 21:56:30');
INSERT INTO `audit_log` VALUES (39, '13800000002', 'DRIVER', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":32,\"plateNo\":\"粤A10002\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"08:00-10:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-10-12 21:56:37');
INSERT INTO `audit_log` VALUES (40, '13800000002', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:57:12');
INSERT INTO `audit_log` VALUES (41, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 61, '2026-10-12 21:57:14');
INSERT INTO `audit_log` VALUES (42, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:57:19');
INSERT INTO `audit_log` VALUES (43, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"gate\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 64, '2026-10-12 21:57:22');
INSERT INTO `audit_log` VALUES (44, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:57:36');
INSERT INTO `audit_log` VALUES (45, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 3, '2026-10-12 21:57:41');
INSERT INTO `audit_log` VALUES (46, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 4, '2026-10-12 21:57:47');
INSERT INTO `audit_log` VALUES (47, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 3, '2026-10-12 21:57:53');
INSERT INTO `audit_log` VALUES (48, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 4, '2026-10-12 21:58:08');
INSERT INTO `audit_log` VALUES (49, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 4, '2026-10-12 21:58:12');
INSERT INTO `audit_log` VALUES (50, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 3, '2026-10-12 21:58:30');
INSERT INTO `audit_log` VALUES (51, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 2, '2026-10-12 21:58:33');
INSERT INTO `audit_log` VALUES (52, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 3, '2026-10-12 21:58:39');
INSERT INTO `audit_log` VALUES (53, 'gate', 'GATE', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:58:45');
INSERT INTO `audit_log` VALUES (54, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company2\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 62, '2026-10-12 21:58:52');
INSERT INTO `audit_log` VALUES (55, 'company2', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:59:04');
INSERT INTO `audit_log` VALUES (56, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 67, '2026-10-12 21:59:05');
INSERT INTO `audit_log` VALUES (57, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/9/pay', '[9]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:59:15');
INSERT INTO `audit_log` VALUES (58, 'company1', 'COMPANY', '缴纳费用', 'POST', '/api/fees/5/pay', '[5]', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-10-12 21:59:17');
INSERT INTO `audit_log` VALUES (59, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 21:59:21');
INSERT INTO `audit_log` VALUES (60, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"gate\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 65, '2026-10-12 21:59:23');
INSERT INTO `audit_log` VALUES (61, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:59:30');
INSERT INTO `audit_log` VALUES (62, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-10-12 21:59:39');
INSERT INTO `audit_log` VALUES (63, 'gate', 'GATE', '出闸办单', 'POST', '/api/gate/checkout', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-10-12 22:00:03');
INSERT INTO `audit_log` VALUES (64, 'gate', 'GATE', '出闸办单', 'POST', '/api/gate/checkout', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 36, '2026-10-12 22:06:48');
INSERT INTO `audit_log` VALUES (65, 'gate', 'GATE', '出闸办单', 'POST', '/api/gate/checkout', '[{\"icCardNo\":\"IC0002\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-10-12 22:07:14');
INSERT INTO `audit_log` VALUES (66, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 5, '2026-10-12 22:07:28');
INSERT INTO `audit_log` VALUES (67, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 0, '该还重单尚未缴费，请通知运输公司缴费后再进闸', 4, '2026-10-12 22:07:32');
INSERT INTO `audit_log` VALUES (68, 'gate', 'GATE', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:15:50');
INSERT INTO `audit_log` VALUES (69, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"gate\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 83, '2026-10-12 22:15:54');
INSERT INTO `audit_log` VALUES (70, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0001\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 18, '2026-10-12 22:16:06');
INSERT INTO `audit_log` VALUES (71, 'gate', 'GATE', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:24:48');
INSERT INTO `audit_log` VALUES (72, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 72, '2026-10-12 22:25:13');
INSERT INTO `audit_log` VALUES (73, 'company1', 'COMPANY', '创建进港预约', 'POST', '/api/appointments', '[{\"eirId\":20,\"plateNo\":\"粤A10001\",\"apptDate\":\"2026-10-13\",\"timeSlot\":\"10:00-12:00\",\"driverId\":null}]', '0:0:0:0:0:0:0:1', 1, NULL, 15, '2026-10-12 22:25:27');
INSERT INTO `audit_log` VALUES (74, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:25:49');
INSERT INTO `audit_log` VALUES (75, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 68, '2026-10-12 22:25:54');
INSERT INTO `audit_log` VALUES (76, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:26:14');
INSERT INTO `audit_log` VALUES (77, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 0, '用户名或密码错误', 3, '2026-10-12 22:26:20');
INSERT INTO `audit_log` VALUES (78, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company2\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 66, '2026-10-12 22:26:24');
INSERT INTO `audit_log` VALUES (79, 'company2', 'COMPANY', '单证派单', 'POST', '/api/eir/39/dispatch', '[39,{\"driverId\":3,\"tractorId\":3}]', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-10-12 22:26:33');
INSERT INTO `audit_log` VALUES (80, 'company2', 'COMPANY', '批量派单', 'POST', '/api/eir/batch-dispatch', '[{\"ids\":[37,35,33,31,30,28,26,24,22],\"driverId\":3,\"tractorId\":3}]', '0:0:0:0:0:0:0:1', 1, NULL, 51, '2026-10-12 22:26:41');
INSERT INTO `audit_log` VALUES (81, 'company2', 'COMPANY', '缴纳费用', 'POST', '/api/fees/15/pay', '[15]', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-10-12 22:26:48');
INSERT INTO `audit_log` VALUES (82, 'company2', 'COMPANY', '缴纳费用', 'POST', '/api/fees/13/pay', '[13]', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-10-12 22:26:49');
INSERT INTO `audit_log` VALUES (83, 'company2', 'COMPANY', '缴纳费用', 'POST', '/api/fees/11/pay', '[11]', '0:0:0:0:0:0:0:1', 1, NULL, 4, '2026-10-12 22:26:51');
INSERT INTO `audit_log` VALUES (84, 'company2', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:27:04');
INSERT INTO `audit_log` VALUES (85, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"gate\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 66, '2026-10-12 22:27:06');
INSERT INTO `audit_log` VALUES (86, 'gate', 'GATE', '进闸办单', 'POST', '/api/gate/checkin', '[{\"icCardNo\":\"IC0003\"}]', '0:0:0:0:0:0:0:1', 0, '该卡对应车辆没有已预约待进闸的单证，禁止进闸', 2, '2026-10-12 22:27:14');
INSERT INTO `audit_log` VALUES (87, 'gate', 'GATE', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:27:27');
INSERT INTO `audit_log` VALUES (88, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 68, '2026-10-12 22:27:33');
INSERT INTO `audit_log` VALUES (89, '13800000001', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:48:23');
INSERT INTO `audit_log` VALUES (90, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 78, '2026-10-12 22:48:26');
INSERT INTO `audit_log` VALUES (91, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 22:53:47');
INSERT INTO `audit_log` VALUES (92, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 72, '2026-10-12 22:53:52');
INSERT INTO `audit_log` VALUES (93, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 95, '2026-10-12 22:58:08');
INSERT INTO `audit_log` VALUES (94, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 75, '2026-10-12 22:59:25');
INSERT INTO `audit_log` VALUES (95, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 23:04:06');
INSERT INTO `audit_log` VALUES (96, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"company1\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 67, '2026-10-12 23:04:07');
INSERT INTO `audit_log` VALUES (97, 'company1', 'COMPANY', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 23:04:14');
INSERT INTO `audit_log` VALUES (98, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"13800000001\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 68, '2026-10-12 23:04:16');
INSERT INTO `audit_log` VALUES (99, '13800000001', 'DRIVER', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 23:04:23');
INSERT INTO `audit_log` VALUES (100, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 67, '2026-10-12 23:04:25');
INSERT INTO `audit_log` VALUES (101, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-12 23:04:32');
INSERT INTO `audit_log` VALUES (102, NULL, NULL, '用户登录', 'POST', '/api/auth/login', '[{\"username\":\"admin\",\"password\":\"***\"}]', '0:0:0:0:0:0:0:1', 1, NULL, 365, '2026-10-13 16:29:07');
INSERT INTO `audit_log` VALUES (103, 'admin', 'ADMIN', '用户登出', 'POST', '/api/auth/logout', NULL, '0:0:0:0:0:0:0:1', 1, NULL, 0, '2026-10-13 16:31:35');

-- ----------------------------
-- Table structure for driver
-- ----------------------------
DROP TABLE IF EXISTS `driver`;
CREATE TABLE `driver`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '司机ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '司机姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号（司机账号用户名与手机号一致）',
  `license_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '驾驶证号',
  `tractor_id` bigint NULL DEFAULT NULL COMMENT '绑定拖车ID',
  `company_id` bigint NOT NULL COMMENT '所属公司ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_company`(`company_id` ASC) USING BTREE,
  INDEX `idx_tractor`(`tractor_id` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '司机表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of driver
-- ----------------------------
INSERT INTO `driver` VALUES (1, '张伟', '13800000001', 'LIC00000001', 1, 1, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `driver` VALUES (2, '陈强', '13800000002', 'LIC00000002', 2, 1, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `driver` VALUES (3, '李明', '13800000003', 'LIC00000003', 3, 2, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');

-- ----------------------------
-- Table structure for eir_order
-- ----------------------------
DROP TABLE IF EXISTS `eir_order`;
CREATE TABLE `eir_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '单证ID',
  `eir_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单证编号（唯一）',
  `business_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：PI-F提重/PI-E提空/GR-E还空/GR-F还重',
  `container_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '集装箱号',
  `container_size` int NOT NULL COMMENT '箱尺寸：20/40',
  `container_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '箱型：GP/HQ',
  `company_id` bigint NOT NULL COMMENT '作业公司ID',
  `tractor_id` bigint NULL DEFAULT NULL COMMENT '派单拖车ID',
  `driver_id` bigint NULL DEFAULT NULL COMMENT '派单司机ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED' COMMENT '单证状态：CREATED已录入/DISPATCHED已派单/APPOINTED已预约/GATE_IN已进闸/GATE_OUT已出闸/DONE已完成/CANCELLED已作废',
  `dispatch_time` datetime NULL DEFAULT NULL COMMENT '派单时间',
  `gate_in_time` datetime NULL DEFAULT NULL COMMENT '进闸时间',
  `gate_out_time` datetime NULL DEFAULT NULL COMMENT '出闸时间',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审单时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_eir_no`(`eir_no` ASC) USING BTREE,
  INDEX `idx_company`(`company_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_tractor`(`tractor_id` ASC) USING BTREE,
  INDEX `idx_driver`(`driver_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'EIR单证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of eir_order
-- ----------------------------
INSERT INTO `eir_order` VALUES (1, 'EIR2026101288001', 'PI-F', 'TEMU8100001', 20, 'GP', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:19:06', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:19:06');
INSERT INTO `eir_order` VALUES (2, 'EIR2026101288002', 'PI-F', 'TEMU8100002', 40, 'HQ', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (3, 'EIR2026101288003', 'PI-F', 'TEMU8100003', 20, 'GP', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:19:06', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:19:06');
INSERT INTO `eir_order` VALUES (4, 'EIR2026101288004', 'PI-F', 'TEMU8100004', 40, 'HQ', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (5, 'EIR2026101288005', 'PI-F', 'TEMU8100005', 20, 'GP', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:18:59', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:59');
INSERT INTO `eir_order` VALUES (6, 'EIR2026101288006', 'PI-F', 'TEMU8100006', 40, 'HQ', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (7, 'EIR2026101288007', 'PI-F', 'TEMU8100007', 20, 'GP', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:18:59', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:59');
INSERT INTO `eir_order` VALUES (8, 'EIR2026101288008', 'PI-F', 'TEMU8100008', 40, 'HQ', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (9, 'EIR2026101288009', 'PI-F', 'TEMU8100009', 20, 'GP', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:18:59', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:59');
INSERT INTO `eir_order` VALUES (10, 'EIR2026101288010', 'PI-F', 'TEMU8100010', 40, 'HQ', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (11, 'EIR2026101288011', 'PI-E', 'CSNU8200001', 20, 'GP', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (12, 'EIR2026101288012', 'PI-E', 'CSNU8200002', 40, 'HQ', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:18:50', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:50');
INSERT INTO `eir_order` VALUES (13, 'EIR2026101288013', 'PI-E', 'CSNU8200003', 20, 'GP', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (14, 'EIR2026101288014', 'PI-E', 'CSNU8200004', 40, 'HQ', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:18:50', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:50');
INSERT INTO `eir_order` VALUES (15, 'EIR2026101288015', 'PI-E', 'CSNU8200005', 20, 'GP', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (16, 'EIR2026101288016', 'PI-E', 'CSNU8200006', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:18:40', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:40');
INSERT INTO `eir_order` VALUES (17, 'EIR2026101288017', 'PI-E', 'CSNU8200007', 20, 'GP', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (18, 'EIR2026101288018', 'PI-E', 'CSNU8200008', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:18:40', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:40');
INSERT INTO `eir_order` VALUES (19, 'EIR2026101288019', 'PI-E', 'CSNU8200009', 20, 'GP', 2, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:10:30');
INSERT INTO `eir_order` VALUES (20, 'EIR2026101288020', 'PI-E', 'CSNU8200010', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-13 16:25:58', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-13 16:25:58');
INSERT INTO `eir_order` VALUES (21, 'EIR2026101288021', 'GR-E', 'UETU8300001', 40, 'HQ', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:18:01', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:01');
INSERT INTO `eir_order` VALUES (22, 'EIR2026101288022', 'GR-E', 'UETU8300002', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (23, 'EIR2026101288023', 'GR-E', 'UETU8300003', 40, 'HQ', 1, 2, 2, 'DISPATCHED', '2026-10-12 21:18:01', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:18:01');
INSERT INTO `eir_order` VALUES (24, 'EIR2026101288024', 'GR-E', 'UETU8300004', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (25, 'EIR2026101288025', 'GR-E', 'UETU8300005', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:17:55', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:17:55');
INSERT INTO `eir_order` VALUES (26, 'EIR2026101288026', 'GR-E', 'UETU8300006', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (27, 'EIR2026101288027', 'GR-E', 'UETU8300007', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-12 21:17:55', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:17:55');
INSERT INTO `eir_order` VALUES (28, 'EIR2026101288028', 'GR-E', 'UETU8300008', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (29, 'EIR2026101288029', 'GR-E', 'UETU8300009', 40, 'HQ', 1, 1, 1, 'GATE_OUT', '2026-10-12 21:17:55', '2026-10-12 21:57:37', '2026-10-12 22:00:03', NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:00:03');
INSERT INTO `eir_order` VALUES (30, 'EIR2026101288030', 'GR-E', 'UETU8300010', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (31, 'EIR2026101288031', 'GR-F', 'TCNU8400001', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (32, 'EIR2026101288032', 'GR-F', 'TCNU8400002', 40, 'HQ', 1, 2, 2, 'GATE_OUT', '2026-10-12 21:17:45', '2026-10-12 21:59:31', '2026-10-12 22:06:49', NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:06:48');
INSERT INTO `eir_order` VALUES (33, 'EIR2026101288033', 'GR-F', 'TCNU8400003', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (34, 'EIR2026101288034', 'GR-F', 'TCNU8400004', 40, 'HQ', 1, 2, 2, 'GATE_OUT', '2026-10-12 21:17:26', '2026-10-12 21:59:40', '2026-10-12 22:07:14', NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:07:14');
INSERT INTO `eir_order` VALUES (35, 'EIR2026101288035', 'GR-F', 'TCNU8400005', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (36, 'EIR2026101288036', 'GR-F', 'TCNU8400006', 40, 'HQ', 1, 1, 1, 'GATE_IN', '2026-10-12 21:17:18', '2026-10-12 22:16:07', NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:16:06');
INSERT INTO `eir_order` VALUES (37, 'EIR2026101288037', 'GR-F', 'TCNU8400007', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:41', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:41');
INSERT INTO `eir_order` VALUES (38, 'EIR2026101288038', 'GR-F', 'TCNU8400008', 40, 'HQ', 1, 1, 1, 'DISPATCHED', '2026-10-13 16:25:58', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-13 16:25:58');
INSERT INTO `eir_order` VALUES (39, 'EIR2026101288039', 'GR-F', 'TCNU8400009', 20, 'GP', 2, 3, 3, 'DISPATCHED', '2026-10-12 22:26:33', NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 22:26:33');
INSERT INTO `eir_order` VALUES (40, 'EIR2026101288040', 'GR-F', 'TCNU8400010', 40, 'HQ', 1, 1, 1, 'APPOINTED', NULL, NULL, NULL, NULL, '测试数据', '2026-10-12 21:10:30', '2026-10-12 21:20:44');

-- ----------------------------
-- Table structure for fee_record
-- ----------------------------
DROP TABLE IF EXISTS `fee_record`;
CREATE TABLE `fee_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收费记录ID',
  `eir_id` bigint NOT NULL COMMENT '关联单证ID',
  `plate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '车牌号',
  `company_id` bigint NOT NULL COMMENT '缴费公司ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '应收金额（固定50元/单，仅GR-F还重单生成）',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '缴费状态：0未缴 1已缴',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '缴费时间',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_eir`(`eir_id` ASC) USING BTREE,
  INDEX `idx_company`(`company_id` ASC) USING BTREE,
  INDEX `idx_pay_status`(`pay_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fee_record
-- ----------------------------
INSERT INTO `fee_record` VALUES (1, 40, '粤A10001', 1, 50.00, 1, '2026-10-12 21:20:02', '第一运输账号', '2026-10-12 21:13:08', '2026-10-12 21:20:02');
INSERT INTO `fee_record` VALUES (2, 38, '粤A10001', 1, 50.00, 1, '2026-10-12 21:19:59', '第一运输账号', '2026-10-12 21:17:18', '2026-10-12 21:19:59');
INSERT INTO `fee_record` VALUES (3, 36, '粤A10001', 1, 50.00, 0, NULL, NULL, '2026-10-12 21:17:18', '2026-10-12 21:17:18');
INSERT INTO `fee_record` VALUES (4, 34, '粤A10002', 1, 50.00, 1, '2026-10-12 21:19:56', '第一运输账号', '2026-10-12 21:17:26', '2026-10-12 21:19:56');
INSERT INTO `fee_record` VALUES (5, 32, '粤A10002', 1, 50.00, 1, '2026-10-12 21:59:17', '第一运输账号', '2026-10-12 21:17:45', '2026-10-12 21:59:17');
INSERT INTO `fee_record` VALUES (6, 9, '粤A10001', 1, 80.00, 1, '2026-10-12 21:19:54', '第一运输账号', '2026-10-12 21:18:59', '2026-10-12 21:19:54');
INSERT INTO `fee_record` VALUES (7, 7, '粤A10001', 1, 80.00, 1, '2026-10-12 21:20:04', '第一运输账号', '2026-10-12 21:18:59', '2026-10-12 21:20:04');
INSERT INTO `fee_record` VALUES (8, 5, '粤A10001', 1, 80.00, 0, NULL, NULL, '2026-10-12 21:18:59', '2026-10-12 21:18:59');
INSERT INTO `fee_record` VALUES (9, 1, '粤A10002', 1, 80.00, 1, '2026-10-12 21:59:15', '第一运输账号', '2026-10-12 21:19:06', '2026-10-12 21:59:15');
INSERT INTO `fee_record` VALUES (10, 3, '粤A10002', 1, 80.00, 1, '2026-10-12 21:19:51', '第一运输账号', '2026-10-12 21:19:06', '2026-10-12 21:19:51');
INSERT INTO `fee_record` VALUES (11, 39, '粤A20001', 2, 50.00, 1, '2026-10-12 22:26:51', '第二运输账号', '2026-10-12 22:26:33', '2026-10-12 22:26:51');
INSERT INTO `fee_record` VALUES (12, 37, '粤A20001', 2, 50.00, 0, NULL, NULL, '2026-10-12 22:26:41', '2026-10-12 22:26:41');
INSERT INTO `fee_record` VALUES (13, 35, '粤A20001', 2, 50.00, 1, '2026-10-12 22:26:49', '第二运输账号', '2026-10-12 22:26:41', '2026-10-12 22:26:49');
INSERT INTO `fee_record` VALUES (14, 33, '粤A20001', 2, 50.00, 0, NULL, NULL, '2026-10-12 22:26:41', '2026-10-12 22:26:41');
INSERT INTO `fee_record` VALUES (15, 31, '粤A20001', 2, 50.00, 1, '2026-10-12 22:26:48', '第二运输账号', '2026-10-12 22:26:41', '2026-10-12 22:26:48');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名（唯一）',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（BCrypt加密）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：ADMIN码头管理员/GATE闸口办单员/COMPANY运输公司用户/DRIVER司机',
  `company_id` bigint NULL DEFAULT NULL COMMENT '所属运输公司ID（码头端用户为空）',
  `driver_id` bigint NULL DEFAULT NULL COMMENT '关联司机ID（仅DRIVER角色，用于数据权限过滤）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_company`(`company_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '系统管理员', 'ADMIN', NULL, NULL, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (2, 'gate', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '闸口办单员', 'GATE', NULL, NULL, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (3, 'company1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '第一运输账号', 'COMPANY', 1, NULL, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (4, 'company2', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '第二运输账号', 'COMPANY', 2, NULL, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (5, '13800000001', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '张伟', 'DRIVER', 1, 1, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (6, '13800000002', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '陈强', 'DRIVER', 1, 2, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `sys_user` VALUES (7, '13800000003', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '李明', 'DRIVER', 2, 3, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');

-- ----------------------------
-- Table structure for tractor
-- ----------------------------
DROP TABLE IF EXISTS `tractor`;
CREATE TABLE `tractor`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '拖车ID',
  `plate_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '拖头车牌号',
  `frame_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '车架号',
  `ic_card_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'IC卡编号（模拟刷卡，唯一）',
  `company_id` bigint NOT NULL COMMENT '所属公司ID',
  `axle_num` int NOT NULL DEFAULT 4 COMMENT '轴数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ic_card_no`(`ic_card_no` ASC) USING BTREE,
  INDEX `idx_company`(`company_id` ASC) USING BTREE,
  INDEX `idx_plate`(`plate_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '拖车表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tractor
-- ----------------------------
INSERT INTO `tractor` VALUES (1, '粤A10001', 'FRAME0000000001', 'IC0001', 1, 4, 1, '2026-10-11 01:21:26', '2026-10-12 18:01:36');
INSERT INTO `tractor` VALUES (2, '粤A10002', 'FRAME0000000002', 'IC0002', 1, 4, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `tractor` VALUES (3, '粤A20001', 'FRAME0000000003', 'IC0003', 2, 6, 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');

-- ----------------------------
-- Table structure for transport_company
-- ----------------------------
DROP TABLE IF EXISTS `transport_company`;
CREATE TABLE `transport_company`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公司ID',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公司编码（唯一）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公司名称',
  `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运输公司表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of transport_company
-- ----------------------------
INSERT INTO `transport_company` VALUES (1, 'COMP001', '第一运输有限公司', '王经理', '13900000001', 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');
INSERT INTO `transport_company` VALUES (2, 'COMP002', '第二运输有限公司', '赵经理', '13900000002', 1, '2026-10-11 01:21:26', '2026-10-11 01:21:26');

SET FOREIGN_KEY_CHECKS = 1;
