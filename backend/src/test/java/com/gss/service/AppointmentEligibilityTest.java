package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.AppointmentDTO;
import com.gss.entity.EirOrder;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.TractorMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.AppointmentStateMachine;
import com.gss.statemachine.EirStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约前置校验（M1/R2）：未审核 / 已驳回 / 未派单 三种原因必须给出不同文案。
 */
@ExtendWith(MockitoExtension.class)
class AppointmentEligibilityTest {

    @Mock private AppointmentMapper appointmentMapper;
    @Mock private ApptSlotCapacityMapper slotCapacityMapper;
    @Mock private EirOrderMapper eirMapper;
    @Mock private TractorMapper tractorMapper;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentMapper, slotCapacityMapper, eirMapper,
                tractorMapper, new EirStateMachine(), new AppointmentStateMachine());
    }

    private LoginUser companyUser() {
        return LoginUser.builder().userId(2L).role("COMPANY").companyId(5L).build();
    }

    private AppointmentDTO dto() {
        AppointmentDTO d = new AppointmentDTO();
        d.setEirId(10L);
        d.setApptDate(LocalDate.now().plusDays(1));
        d.setTimeSlot("08:00-10:00");
        return d;
    }

    private EirOrder eir(String status, String rejectReason) {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setStatus(status);
        e.setCompanyId(5L);
        e.setRejectReason(rejectReason);
        return e;
    }

    @Test
    void pendingReviewCannotBook() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_SUBMITTED, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(dto(), companyUser()));

        assertTrue(error.getMessage().contains("尚未审核"));
        verify(appointmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectedCannotBookAndReasonShown() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_REJECTED, "箱号与单据不一致"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(dto(), companyUser()));

        assertTrue(error.getMessage().contains("已被驳回"));
        assertTrue(error.getMessage().contains("箱号与单据不一致"));
        verify(appointmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approvedButNotDispatchedCannotBook() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPROVED, null));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(dto(), companyUser()));

        assertTrue(error.getMessage().contains("尚未派单"));
        verify(appointmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }
}