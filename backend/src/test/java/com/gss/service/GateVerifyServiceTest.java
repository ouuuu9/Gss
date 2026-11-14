package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.GateVerifyDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.GateCheckLog;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.GateCheckLogMapper;
import com.gss.mapper.TractorMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.AppointmentStateMachine;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.VerifyResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 闸口凭证核验测试（验收 2–7）：判定顺序 a–f、幂等、越权拦截、留痕。
 * 固定时钟 2026-11-15 09:00（落在 08:00-10:00 的窗口内）。
 */
@ExtendWith(MockitoExtension.class)
class GateVerifyServiceTest {

    @Mock private TractorMapper tractorMapper;
    @Mock private EirOrderMapper eirMapper;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private FeeRecordMapper feeMapper;
    @Mock private GateCheckLogMapper checkLogMapper;

    private GateService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final LocalDateTime FIXED_NOW = TODAY.atTime(9, 0);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @BeforeEach
    void setUp() {
        service = new GateService(tractorMapper, eirMapper, appointmentMapper, feeMapper,
                new EirStateMachine(), new AppointmentStateMachine(), checkLogMapper);
        service.setClock(Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE));
    }

    private LoginUser gateUser() {
        return LoginUser.builder().userId(1L).role("GATE").build();
    }

    private LoginUser companyGate() {
        return LoginUser.builder().userId(2L).role("GATE").companyId(5L).build();
    }

    private Appointment appt(String status, String slot, LocalDate date) {
        Appointment a = new Appointment();
        a.setId(100L);
        a.setEirId(10L);
        a.setVoucherNo("APT20261115000001");
        a.setStatus(status);
        a.setApptDate(date == null ? TODAY : date);
        a.setTimeSlot(slot == null ? "08:00-10:00" : slot);
        a.setDriverId(5L);
        return a;
    }

    private EirOrder eir(String status, Long companyId, Long tractorId) {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setEirNo("EIR20261115001");
        e.setStatus(status);
        e.setCompanyId(companyId == null ? 5L : companyId);
        e.setTractorId(tractorId == null ? 7L : tractorId);
        return e;
    }

    private GateVerifyDTO byVoucher() {
        GateVerifyDTO dto = new GateVerifyDTO();
        dto.setVoucherNo("APT20261115000001");
        return dto;
    }

    private GateVerifyDTO byIcCard(String card) {
        GateVerifyDTO dto = new GateVerifyDTO();
        dto.setIcCard(card);
        return dto;
    }

    private GateCheckLog capturedLog() {
        ArgumentCaptor<GateCheckLog> captor = ArgumentCaptor.forClass(GateCheckLog.class);
        verify(checkLogMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getValue();
    }

    /** 验收 2：凭证有效且在窗口内 → PASS，预约变 VERIFIED，留痕 PASS */
    @Test
    void verifyPassWithinWindow() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001")).thenReturn(appt(Constants.APPT_ACTIVE, null, null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));
        when(appointmentMapper.markVerified(100L, Constants.APPT_VERIFIED, Constants.APPT_ACTIVE,
                FIXED_NOW)).thenReturn(1);

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.CHECK_PASS, result.getResult());
        verify(appointmentMapper).markVerified(100L, Constants.APPT_VERIFIED, Constants.APPT_ACTIVE, FIXED_NOW);
        assertEquals(Constants.CHECK_PASS, capturedLog().getResult());
    }

    /** 验收 3：早到 → EARLY */
    @Test
    void verifyEarly() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_ACTIVE, "16:00-18:00", null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.CHECK_FAIL, result.getResult());
        assertEquals(Constants.ABNORMAL_EARLY, result.getAbnormalType());
        verify(appointmentMapper, never()).markVerified(anyLong(), any(), any(), any());
    }

    /** 验收 3：晚到 → LATE */
    @Test
    void verifyLate() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_ACTIVE, "06:00-08:00", null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.ABNORMAL_LATE, result.getAbnormalType());
    }

    /** 验收 4：已取消 → CANCELLED；已失约 → EXPIRED */
    @Test
    void verifyCancelledAndNoShow() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_CANCELLED, null, null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));
        assertEquals(Constants.ABNORMAL_CANCELLED, service.verify(byVoucher(), gateUser()).getAbnormalType());

        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_NO_SHOW, null, null));
        assertEquals(Constants.ABNORMAL_EXPIRED, service.verify(byVoucher(), gateUser()).getAbnormalType());
    }

    /** 验收 5：单证状态不匹配 → DOC_MISMATCH */
    @Test
    void verifyDocMismatch() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001")).thenReturn(appt(Constants.APPT_ACTIVE, null, null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_DISPATCHED, null, null));

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.ABNORMAL_DOC_MISMATCH, result.getAbnormalType());
    }

    /** 验收 5：IC 卡车辆与派单车辆不一致 → DOC_MISMATCH */
    @Test
    void verifyVehicleMismatch() {
        Tractor tractor = new Tractor();
        tractor.setId(99L);
        when(tractorMapper.selectByIcCardNo("IC9999")).thenReturn(tractor);
        when(appointmentMapper.selectActiveByTractorId(99L))
                .thenReturn(List.of(appt(Constants.APPT_ACTIVE, null, null)));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, 7L));

        VerifyResultVO result = service.verify(byIcCard("IC9999"), gateUser());

        assertEquals(Constants.ABNORMAL_DOC_MISMATCH, result.getAbnormalType());
    }

    /** 验收 6：重复核验 → 幂等 PASS，不再改状态 */
    @Test
    void verifyTwiceIsIdempotent() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_VERIFIED, null, null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.CHECK_PASS, result.getResult());
        verify(appointmentMapper, never()).markVerified(anyLong(), any(), any(), any());
        assertEquals(Constants.CHECK_PASS, capturedLog().getResult());
    }

    /** 验收 7：公司闸口账号核验其他公司单证 → 越权拦截 */
    @Test
    void verifyOtherCompanyRejected() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001")).thenReturn(appt(Constants.APPT_ACTIVE, null, null));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, 6L, null));

        assertThrows(BusinessException.class, () -> service.verify(byVoucher(), companyGate()));
        verify(appointmentMapper, never()).markVerified(anyLong(), any(), any(), any());
    }

    /** 查不到预约 → NO_APPOINTMENT，且留痕 */
    @Test
    void verifyNoAppointment() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001")).thenReturn(null);

        VerifyResultVO result = service.verify(byVoucher(), gateUser());

        assertEquals(Constants.ABNORMAL_NO_APPOINTMENT, result.getAbnormalType());
        assertEquals(Constants.CHECK_FAIL, capturedLog().getResult());
    }

    /** 预约日期不是今天 → EXPIRED */
    @Test
    void verifyNotToday() {
        when(appointmentMapper.selectByVoucherNo("APT20261115000001"))
                .thenReturn(appt(Constants.APPT_ACTIVE, null, TODAY.plusDays(1)));
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED, null, null));

        assertEquals(Constants.ABNORMAL_EXPIRED, service.verify(byVoucher(), gateUser()).getAbnormalType());
    }
}