package com.gss.statemachine;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M1 状态机转移表测试：审核闭环新增的 SUBMITTED / APPROVED / REJECTED 三态，
 * 以及「未审核不可派单」「取消派单回到 APPROVED」两条硬约束。
 */
class EirApprovalStateMachineTest {

    private final EirStateMachine machine = new EirStateMachine();

    @Test
    void createdCanSubmit() {
        assertEquals(Constants.ST_SUBMITTED, machine.validateAndTransition(Constants.ST_CREATED, EirEvent.SUBMIT));
    }

    /** 管理员代录：CREATED → APPROVED */
    @Test
    void createdCanBeApprovedDirectly() {
        assertEquals(Constants.ST_APPROVED, machine.validateAndTransition(Constants.ST_CREATED, EirEvent.APPROVE));
    }

    /** R2：未审核不可派单（CREATED 直接派单必须被拒绝） */
    @Test
    void createdCannotDispatchWithoutReview() {
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.ST_CREATED, EirEvent.DISPATCH));
    }

    @Test
    void submittedCanBeApprovedOrRejected() {
        assertEquals(Constants.ST_APPROVED, machine.validateAndTransition(Constants.ST_SUBMITTED, EirEvent.APPROVE));
        assertEquals(Constants.ST_REJECTED, machine.validateAndTransition(Constants.ST_SUBMITTED, EirEvent.REJECT));
    }

    @Test
    void submittedCannotDispatchDirectly() {
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.ST_SUBMITTED, EirEvent.DISPATCH));
    }

    @Test
    void rejectedCanResubmit() {
        assertEquals(Constants.ST_SUBMITTED, machine.validateAndTransition(Constants.ST_REJECTED, EirEvent.SUBMIT));
    }

    @Test
    void approvedCanDispatch() {
        assertEquals(Constants.ST_DISPATCHED, machine.validateAndTransition(Constants.ST_APPROVED, EirEvent.DISPATCH));
    }

    /** 取消派单回到 APPROVED（不再回到 CREATED，避免绕过审核） */
    @Test
    void cancelDispatchReturnsToApproved() {
        assertEquals(Constants.ST_APPROVED,
                machine.validateAndTransition(Constants.ST_DISPATCHED, EirEvent.CANCEL_DISPATCH));
    }

    /** 撤销审核：已通过 / 已派单 / 已预约 三种状态都能回退到 REJECTED */
    @Test
    void revokeReviewFromApprovedDispatchedAppointed() {
        assertEquals(Constants.ST_REJECTED,
                machine.validateAndTransition(Constants.ST_APPROVED, EirEvent.REVOKE_REVIEW));
        assertEquals(Constants.ST_REJECTED,
                machine.validateAndTransition(Constants.ST_DISPATCHED, EirEvent.REVOKE_REVIEW));
        assertEquals(Constants.ST_REJECTED,
                machine.validateAndTransition(Constants.ST_APPOINTED, EirEvent.REVOKE_REVIEW));
    }

    /** 撤销审核的边界：待审核不能撤销（用驳回），已进闸不能撤销 */
    @Test
    void revokeReviewBoundaries() {
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.ST_SUBMITTED, EirEvent.REVOKE_REVIEW));
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.ST_GATE_IN, EirEvent.REVOKE_REVIEW));
        assertThrows(BusinessException.class,
                () -> machine.validateAndTransition(Constants.ST_REJECTED, EirEvent.REVOKE_REVIEW));
    }

    /** 未进闸各态均可作废 */
    @Test
    void newStatesAreCancellable() {
        assertTrue(machine.canCancel(Constants.ST_SUBMITTED));
        assertTrue(machine.canCancel(Constants.ST_APPROVED));
        assertTrue(machine.canCancel(Constants.ST_REJECTED));
        // 已进闸仍不可作废
        assertFalse(machine.canCancel(Constants.ST_GATE_IN));
    }
}