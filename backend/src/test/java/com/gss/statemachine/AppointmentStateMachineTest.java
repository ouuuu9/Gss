package com.gss.statemachine;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 预约状态机测试：ACTIVE → VERIFIED → RELEASED，以及取消/失约两条旁路。
 */
class AppointmentStateMachineTest {

    private final AppointmentStateMachine machine = new AppointmentStateMachine();

    @Test
    void activeCanVerify() {
        assertEquals(Constants.APPT_VERIFIED,
                machine.validateAndTransition(Constants.APPT_ACTIVE, AppointmentEvent.VERIFY));
    }

    @Test
    void verifiedCanRelease() {
        assertEquals(Constants.APPT_RELEASED,
                machine.validateAndTransition(Constants.APPT_VERIFIED, AppointmentEvent.RELEASE));
    }

    @Test
    void activeCanCancelOrExpire() {
        assertEquals(Constants.APPT_CANCELLED,
                machine.validateAndTransition(Constants.APPT_ACTIVE, AppointmentEvent.CANCEL));
        assertEquals(Constants.APPT_NO_SHOW,
                machine.validateAndTransition(Constants.APPT_ACTIVE, AppointmentEvent.EXPIRE));
    }

    /** 未核验不能直接放行；已放行不能重复放行 */
    @Test
    void releaseRequiresVerified() {
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.APPT_ACTIVE, AppointmentEvent.RELEASE));
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.APPT_RELEASED, AppointmentEvent.RELEASE));
    }

    /** 终态不可再流转 */
    @Test
    void terminalStatesAreClosed() {
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.APPT_CANCELLED, AppointmentEvent.VERIFY));
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.APPT_NO_SHOW, AppointmentEvent.VERIFY));
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.APPT_EXPIRED, AppointmentEvent.VERIFY));
    }

    @Test
    void hasVerifiedCoversVerifiedAndReleased() {
        assertTrue(machine.hasVerified(Constants.APPT_VERIFIED));
        assertTrue(machine.hasVerified(Constants.APPT_RELEASED));
        assertFalse(machine.hasVerified(Constants.APPT_ACTIVE));
    }
}