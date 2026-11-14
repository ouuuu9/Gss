package com.gss.service;

import com.gss.common.Constants;
import com.gss.dto.AppointmentDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.GateLane;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.GateLaneMapper;
import com.gss.mapper.TractorMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.AppointmentStateMachine;
import com.gss.statemachine.EirStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 凭证号生成测试（验收 1）：
 * 格式 APT + yyyyMMdd + 6 位当日序列，序列递增；唯一索引冲突时重新取号重试一次。
 */
@ExtendWith(MockitoExtension.class)
class AppointmentVoucherTest {

    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ApptSlotCapacityMapper slotCapacityMapper;
    @Mock private EirOrderMapper eirMapper;
    @Mock private TractorMapper tractorMapper;
    @Mock private GateLaneMapper laneMapper;

    private AppointmentService service;

    private static final LocalDate APPT_DATE = LocalDate.now().plusDays(1);
    /** 凭证号日期取「生成当日」，不是预约日期 */
    private static final LocalDate ISSUE_DAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentMapper, slotCapacityMapper, eirMapper,
                tractorMapper, laneMapper, new EirStateMachine(), new AppointmentStateMachine());
    }

    private LoginUser driverUser() {
        return LoginUser.builder().userId(9L).role("DRIVER").driverId(5L).companyId(1L).build();
    }

    private AppointmentDTO dto() {
        AppointmentDTO d = new AppointmentDTO();
        d.setEirId(10L);
        d.setApptDate(APPT_DATE);
        d.setTimeSlot("08:00-10:00");
        d.setPlateNo("粤A10001");
        d.setLaneId(1L);
        return d;
    }

    private GateLane enabledLane() {
        GateLane lane = new GateLane();
        lane.setId(1L);
        lane.setGateNo("GATE-1");
        lane.setLaneNo("LANE-1");
        lane.setCapacityPerSlot(10);
        lane.setStatus(Constants.LANE_ENABLED);
        return lane;
    }

    private EirOrder eir() {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setStatus(Constants.ST_DISPATCHED);
        e.setDriverId(5L);
        e.setTractorId(7L);
        e.setCompanyId(1L);
        return e;
    }

    @Test
    void voucherFollowsDailySequence() {
        when(eirMapper.selectById(10L)).thenReturn(eir());
        when(appointmentMapper.selectMaxVoucherSeq(anyString())).thenReturn(41);
        when(laneMapper.selectById(1L)).thenReturn(enabledLane());
        when(slotCapacityMapper.tryBook(1L, APPT_DATE, "08:00-10:00")).thenReturn(1);
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.create(dto(), driverUser());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).insert(captor.capture());
        String expectedPrefix = "APT" + ISSUE_DAY.format(DateTimeFormatter.BASIC_ISO_DATE);
        assertEquals(expectedPrefix + "000042", captor.getValue().getVoucherNo());
        assertEquals(Constants.APPT_ACTIVE, captor.getValue().getStatus());
    }

    /** 唯一索引冲突：重新取号并重试一次（并发建单场景） */
    @Test
    void duplicateVoucherRetriesOnce() {
        when(eirMapper.selectById(10L)).thenReturn(eir());
        // 第一次取到 41，撞号后重新取到 42
        when(appointmentMapper.selectMaxVoucherSeq(anyString())).thenReturn(41, 42);
        when(laneMapper.selectById(1L)).thenReturn(enabledLane());
        when(slotCapacityMapper.tryBook(1L, APPT_DATE, "08:00-10:00")).thenReturn(1);
        when(appointmentMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("uk_voucher_no"))
                .thenReturn(1);
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.create(dto(), driverUser());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper, times(2)).insert(captor.capture());
        String expectedPrefix = "APT" + ISSUE_DAY.format(DateTimeFormatter.BASIC_ISO_DATE);
        // 第二次插入用的号必须重新生成
        assertEquals(expectedPrefix + "000043", captor.getAllValues().get(1).getVoucherNo());
    }
}