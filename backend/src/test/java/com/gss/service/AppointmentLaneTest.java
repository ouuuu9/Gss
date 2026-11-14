package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.AppointmentDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.GateLane;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3 预约车道测试（验收 3、5）：
 * 车道必填、停用车道的预约被拒、扣减落到指定车道。
 */
@ExtendWith(MockitoExtension.class)
class AppointmentLaneTest {

    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ApptSlotCapacityMapper slotCapacityMapper;
    @Mock private EirOrderMapper eirMapper;
    @Mock private TractorMapper tractorMapper;
    @Mock private GateLaneMapper laneMapper;

    private AppointmentService service;

    private static final LocalDate APPT_DATE = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentMapper, slotCapacityMapper, eirMapper,
                tractorMapper, laneMapper, new EirStateMachine(), new AppointmentStateMachine());
    }

    private LoginUser driverUser() {
        return LoginUser.builder().userId(9L).role("DRIVER").driverId(5L).companyId(1L).build();
    }

    private AppointmentDTO dto(Long laneId) {
        AppointmentDTO d = new AppointmentDTO();
        d.setEirId(10L);
        d.setLaneId(laneId);
        d.setApptDate(APPT_DATE);
        d.setTimeSlot("08:00-10:00");
        d.setPlateNo("粤A10001");
        return d;
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

    private GateLane lane(Long id, String status) {
        GateLane l = new GateLane();
        l.setId(id);
        l.setLaneNo("LANE-" + id);
        l.setCapacityPerSlot(10);
        l.setStatus(status);
        return l;
    }

    /** 停用车道不能预约 */
    @Test
    void disabledLaneRejected() {
        when(eirMapper.selectById(10L)).thenReturn(eir());
        when(laneMapper.selectById(2L)).thenReturn(lane(2L, Constants.LANE_DISABLED));

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto(2L), driverUser()));

        assertTrue(error.getMessage().contains("已停用"));
        verify(slotCapacityMapper, never()).tryBook(any(), any(), any());
        verify(appointmentMapper, never()).insert(any());
    }

    /** 指定车道预约：容量扣减与预约记录都落到该车道 */
    @Test
    void booksOnSelectedLane() {
        when(eirMapper.selectById(10L)).thenReturn(eir());
        when(laneMapper.selectById(1L)).thenReturn(lane(1L, Constants.LANE_ENABLED));
        when(slotCapacityMapper.tryBook(1L, APPT_DATE, "08:00-10:00")).thenReturn(1);
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.create(dto(1L), driverUser());

        verify(slotCapacityMapper).insertIgnore(1L, APPT_DATE, "08:00-10:00", 10);
        verify(slotCapacityMapper).tryBook(1L, APPT_DATE, "08:00-10:00");
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getLaneId());
    }

    /** 该车道时段已满：提示指定车道已满，不写预约 */
    @Test
    void fullLaneRejected() {
        when(eirMapper.selectById(10L)).thenReturn(eir());
        when(laneMapper.selectById(1L)).thenReturn(lane(1L, Constants.LANE_ENABLED));
        when(slotCapacityMapper.tryBook(1L, APPT_DATE, "08:00-10:00")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto(1L), driverUser()));

        assertTrue(error.getMessage().contains("已满"));
        verify(appointmentMapper, never()).insert(any());
    }
}