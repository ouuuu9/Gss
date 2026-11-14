package com.gss.service;

import com.gss.common.Constants;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.statemachine.AppointmentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约失效级联单元测试（评审意见 3）：
 * 验证「取消 ACTIVE 预约 + 回补名额」只在 CAS 命中一行时发生，且重复调用不会重复回补。
 */
@ExtendWith(MockitoExtension.class)
class EirCancellerTest {

    @Mock private EirOrderMapper eirMapper;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ApptSlotCapacityMapper slotCapacityMapper;

    private EirCanceller canceller;

    private static final LocalDate APPT_DATE = LocalDate.of(2026, 12, 1);
    private static final String TIME_SLOT = "08:00-10:00";

    @BeforeEach
    void setUp() {
        canceller = new EirCanceller(eirMapper, appointmentMapper, slotCapacityMapper,
                new AppointmentStateMachine());
    }

    private EirOrder eir() {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setStatus(Constants.ST_APPOINTED);
        return e;
    }

    private Appointment activeAppointment() {
        Appointment a = new Appointment();
        a.setId(100L);
        a.setEirId(10L);
        a.setStatus(Constants.APPT_ACTIVE);
        a.setApptDate(APPT_DATE);
        a.setTimeSlot(TIME_SLOT);
        return a;
    }

    /** 预约 ACTIVE 且 booked_count=N：调用后预约转 CANCELLED，名额归还一次（N-1） */
    @Test
    void activeAppointmentReleasedOnce() {
        Appointment appt = activeAppointment();
        when(appointmentMapper.selectActiveByEirId(10L)).thenReturn(appt);
        // 条件更新命中一行，代表本次调用成功把预约从 ACTIVE 抢成 CANCELLED
        when(appointmentMapper.updateStatusIf(100L, Constants.APPT_CANCELLED, Constants.APPT_ACTIVE))
                .thenReturn(1);

        canceller.releaseActiveAppointment(eir());

        verify(appointmentMapper).updateStatusIf(100L, Constants.APPT_CANCELLED, Constants.APPT_ACTIVE);
        verify(slotCapacityMapper).release(APPT_DATE, TIME_SLOT);
    }

    /** 重复调用（预约已不是 ACTIVE）：查不到有效预约，不再回补名额 */
    @Test
    void secondCallDoesNotReleaseAgain() {
        // 第一次调用后预约已 CANCELLED；selectActiveByEirId 只返回 ACTIVE，因此第二次返回 null
        when(appointmentMapper.selectActiveByEirId(10L)).thenReturn(null);

        canceller.releaseActiveAppointment(eir());

        verify(appointmentMapper, never()).updateStatusIf(any(), any(), any());
        verify(slotCapacityMapper, never()).release(any(), any());
    }

    /** 并发下 CAS 没抢到（返回 0）：说明别人已经取消过，不能再释放名额 */
    @Test
    void noReleaseWhenCasMisses() {
        when(appointmentMapper.selectActiveByEirId(10L)).thenReturn(activeAppointment());
        when(appointmentMapper.updateStatusIf(100L, Constants.APPT_CANCELLED, Constants.APPT_ACTIVE))
                .thenReturn(0);

        canceller.releaseActiveAppointment(eir());

        verify(slotCapacityMapper, never()).release(any(), any());
    }

    /** 预约时段为空（历史脏数据）：状态照常取消，但不能拿空时段去释放名额 */
    @Test
    void noReleaseWhenTimeSlotBlank() {
        Appointment appt = activeAppointment();
        appt.setTimeSlot(null);
        when(appointmentMapper.selectActiveByEirId(10L)).thenReturn(appt);
        when(appointmentMapper.updateStatusIf(100L, Constants.APPT_CANCELLED, Constants.APPT_ACTIVE))
                .thenReturn(1);

        canceller.releaseActiveAppointment(eir());

        verify(slotCapacityMapper, never()).release(any(), any());
    }
}