package com.gss.service;

import com.gss.common.Constants;
import com.gss.dto.AppointmentDTO;
import com.gss.dto.EirApplyDTO;
import com.gss.dto.GateVerifyDTO;
import com.gss.dto.LaneCapacityBatchDTO;
import com.gss.dto.StatsQueryDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.GateLane;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.GateLaneMapper;
import com.gss.security.LoginUser;
import com.gss.vo.StatsResultVO;
import com.gss.vo.VerifyResultVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M1–M4 真库集成测试（M5）。
 *
 * 与单元测试的区别：这里跑真实 MyBatis SQL + 真实事务，专门覆盖那些
 * 「Mockito 绿但 SQL 可能写错」的路径（M1 的 P0 就是这么漏掉的）。
 *
 * 运行方式：mvn -o test -Pintegration（需要 3307 上的库）
 * 默认 mvn test 通过 @Tag("integration") 排除本类。
 * 每个用例在 @Transactional 中执行并自动回滚，不污染库。
 */
@SpringBootTest
@Tag("integration")
@Transactional
class M1ToM4IntegrationTest {

    @Autowired private EirService eirService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private GateService gateService;
    @Autowired private StatsService statsService;
    @Autowired private EirOrderMapper eirMapper;
    @Autowired private AppointmentMapper appointmentMapper;
    @Autowired private GateLaneMapper laneMapper;
    @Autowired private ApptSlotCapacityMapper capacityMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private LoginUser company() {
        return LoginUser.builder().userId(3L).role("COMPANY").companyId(1L).name("集成测试公司").build();
    }

    private LoginUser admin() {
        return LoginUser.builder().userId(1L).role("ADMIN").name("集成管理员").build();
    }

    private LoginUser gate() {
        return LoginUser.builder().userId(2L).role("GATE").name("集成闸口").build();
    }

    private String uniq(String prefix) {
        return prefix + (System.nanoTime() % 100000000L);
    }

    /** M1：客户申请 → 管理员审核通过，真实 SQL 落库（验证 updateStatus 拼写与 CAS） */
    @Test
    void m1_applyAndApprovePersist() {
        EirApplyDTO dto = new EirApplyDTO();
        dto.setBusinessType("PI-E");
        dto.setContainerNo(uniq("IT"));
        dto.setContainerSize(20);
        dto.setContainerType("GP");
        eirService.apply(dto, company());

        Long createdId = jdbcTemplate.queryForObject(
                "SELECT id FROM eir_order WHERE container_no = ? ORDER BY id DESC LIMIT 1", Long.class, dto.getContainerNo());
        assertNotNull(createdId);
        EirOrder row = eirMapper.selectById(createdId);
        assertEquals(Constants.ST_SUBMITTED, row.getStatus());
        assertEquals(Constants.REVIEW_PENDING, row.getReviewStatus());

        eirService.approve(createdId, admin());
        EirOrder approved = eirMapper.selectById(createdId);
        assertEquals(Constants.ST_APPROVED, approved.getStatus());
        assertEquals(Constants.REVIEW_APPROVED, approved.getReviewStatus());
        assertNotNull(approved.getReviewTime());
    }

    /** M2：真实 SQL 下 预约→凭证→核验 PASS→留痕（含 markVerified 的 CAS） */
    @Test
    void m2_voucherVerifyPersists() {
        // 固定时钟到 09:00，配合 08:00-10:00 时段
        gateService.setClock(Clock.fixed(LocalDateTime.of(2026, 11, 20, 9, 0).atZone(ZONE).toInstant(), ZONE));

        EirOrder eir = new EirOrder();
        eir.setEirNo(uniq("ITEIR"));
        eir.setBusinessType("PI-E");
        eir.setContainerNo(uniq("ITCN"));
        eir.setContainerSize(20);
        eir.setContainerType("GP");
        eir.setCompanyId(1L);
        eir.setStatus(Constants.ST_DISPATCHED);
        eir.setDriverId(1L);
        eir.setTractorId(1L);
        eir.setReviewStatus(Constants.REVIEW_APPROVED);
        jdbcTemplate.update("INSERT INTO eir_order (eir_no, business_type, container_no, container_size, container_type,"
                        + " company_id, status, driver_id, tractor_id, review_status) VALUES (?,?,?,?,?,?,?,?,?,?)",
                eir.getEirNo(), eir.getBusinessType(), eir.getContainerNo(), 20, "GP", 1L,
                Constants.ST_DISPATCHED, 1L, 1L, Constants.REVIEW_APPROVED);
        Long eirId = jdbcTemplate.queryForObject("SELECT id FROM eir_order WHERE eir_no = ?", Long.class, eir.getEirNo());

        AppointmentDTO apptDto = new AppointmentDTO();
        apptDto.setEirId(eirId);
        apptDto.setLaneId(1L);
        apptDto.setApptDate(LocalDate.of(2026, 11, 20));
        apptDto.setTimeSlot("08:00-10:00");
        apptDto.setPlateNo("粤A10001");
        appointmentService.create(apptDto, company());

        Appointment saved = appointmentMapper.selectByEirIdAndStatus(eirId, Constants.APPT_ACTIVE);
        assertNotNull(saved);
        assertNotNull(saved.getVoucherNo());
        assertTrue(saved.getVoucherNo().startsWith("APT"));

        GateVerifyDTO verify = new GateVerifyDTO();
        verify.setVoucherNo(saved.getVoucherNo());
        VerifyResultVO result = gateService.verify(verify, gate());
        assertEquals(Constants.CHECK_PASS, result.getResult());

        assertEquals(Constants.APPT_VERIFIED,
                appointmentMapper.selectById(saved.getId()).getStatus());
        Long passLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM gate_check_log WHERE appointment_id = ? AND result = 'PASS'",
                Long.class, saved.getId());
        assertEquals(1L, passLogs);
    }

    /** M3：容量按车道隔离——同日期同时段，A 满不影响 B（真库 unique key + 条件 UPDATE） */
    @Test
    void m3_laneCapacityIsolated() {
        LocalDate date = LocalDate.of(2026, 11, 21);
        capacityMapper.insertIgnore(1L, date, "08:00-10:00", 1);
        capacityMapper.insertIgnore(2L, date, "08:00-10:00", 1);

        assertEquals(1, capacityMapper.tryBook(1L, date, "08:00-10:00"));
        assertEquals(0, capacityMapper.tryBook(1L, date, "08:00-10:00"));
        assertEquals(1, capacityMapper.tryBook(2L, date, "08:00-10:00"));

        Integer booked1 = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM appt_slot_capacity WHERE lane_id=1 AND appt_date=? AND time_slot='08:00-10:00'",
                Integer.class, date);
        Integer booked2 = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM appt_slot_capacity WHERE lane_id=2 AND appt_date=? AND time_slot='08:00-10:00'",
                Integer.class, date);
        assertEquals(1, booked1);
        assertEquals(1, booked2);

        // 释放只影响自己车道
        capacityMapper.release(1L, date, "08:00-10:00");
        assertEquals(0, (int) jdbcTemplate.queryForObject(
                "SELECT booked_count FROM appt_slot_capacity WHERE lane_id=1 AND appt_date=? AND time_slot='08:00-10:00'",
                Integer.class, date));
        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT booked_count FROM appt_slot_capacity WHERE lane_id=2 AND appt_date=? AND time_slot='08:00-10:00'",
                Integer.class, date));
    }

    /** M4：统计口径在真实 SQL 下与手算一致（含重复核验排除、分母排除取消） */
    @Test
    void m4_statsMatchManualCalculation() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        // 4 条预约：1 取消 / 1 失约 / 1 放行(准时) / 1 放行(晚到)
        jdbcTemplate.update("INSERT INTO appointment (eir_id, plate_no, appt_date, time_slot, status, driver_id, voucher_no, lane_id, verify_time, release_time) VALUES "
                + "(101,'P1',?,'08:00-10:00','CANCELLED',1,'ITV1',1,NULL,NULL),"
                + "(102,'P1',?,'08:00-10:00','NO_SHOW',1,'ITV2',1,NULL,NULL),"
                + "(103,'P1',?,'08:00-10:00','RELEASED',1,'ITV3',1,'2026-10-01 07:40:00','2026-10-01 09:10:00'),"
                + "(104,'P1',?,'08:00-10:00','RELEASED',1,'ITV4',1,'2026-10-01 09:30:00','2026-10-01 10:10:00')",
                d, d, d, d);
        // 核验记录：2 条正常 PASS（其中 1 条重复核验要排除）+ 1 条 FAIL
        jdbcTemplate.update("INSERT INTO gate_check_log (voucher_no, appointment_id, check_time, result, abnormal_type, operator_id, remark) VALUES "
                + "('ITV3', (SELECT id FROM (SELECT id FROM appointment WHERE voucher_no='ITV3') t3), '2026-10-01 07:40:00','PASS',NULL,1,'IC卡=A'),"
                + "('ITV3', (SELECT id FROM (SELECT id FROM appointment WHERE voucher_no='ITV3') t3b), '2026-10-01 07:50:00','PASS',NULL,1,'该预约已核验（重复核验）'),"
                + "('ITV2', (SELECT id FROM (SELECT id FROM appointment WHERE voucher_no='ITV2') t2), '2026-10-01 12:30:00','FAIL','EXPIRED',1,'IC卡=B')");

        StatsQueryDTO q = new StatsQueryDTO();
        q.setStartDate(d);
        q.setEndDate(d);
        StatsResultVO stats = statsService.stats(q, admin());

        assertEquals(4L, stats.getSummary().getTotal());
        assertEquals(1L, stats.getSummary().getCancelled());
        assertEquals(1L, stats.getSummary().getNoShow());
        assertEquals(2L, stats.getSummary().getReleased());
        assertEquals(1L, stats.getSummary().getPassCount(), "重复核验必须排除");
        assertEquals(1L, stats.getSummary().getFailCount());
        assertEquals(1L, stats.getSummary().getOnTimeCount());
        // 失约率 = 1 / (4-1) = 33.33；准点率 = 1/1 = 100；放行率 = 2/3 = 66.67
        assertEquals(33.33, stats.getSummary().getNoShowRate());
        assertEquals(100.0, stats.getSummary().getPunctualRate());
        assertEquals(66.67, stats.getSummary().getReleaseRate());
    }
}