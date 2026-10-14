package com.gss.statemachine;

import com.gss.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EIR 单证状态机引擎单元测试
 */
@DisplayName("EIR状态机引擎")
class EirStateMachineTest {

    private EirStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new EirStateMachine();
    }

    @Nested
    @DisplayName("合法转移")
    class ValidTransitions {

        @Test
        @DisplayName("CREATED + DISPATCH → DISPATCHED")
        void created_dispatch() {
            assertEquals("DISPATCHED", sm.validateAndTransition("CREATED", EirEvent.DISPATCH));
        }

        @Test
        @DisplayName("DISPATCHED + CANCEL_DISPATCH → CREATED")
        void dispatched_cancelDispatch() {
            assertEquals("CREATED", sm.validateAndTransition("DISPATCHED", EirEvent.CANCEL_DISPATCH));
        }

        @Test
        @DisplayName("DISPATCHED + APPOINT → APPOINTED")
        void dispatched_appoint() {
            assertEquals("APPOINTED", sm.validateAndTransition("DISPATCHED", EirEvent.APPOINT));
        }

        @Test
        @DisplayName("APPOINTED + CANCEL_APPOINT → DISPATCHED")
        void appointed_cancelAppoint() {
            assertEquals("DISPATCHED", sm.validateAndTransition("APPOINTED", EirEvent.CANCEL_APPOINT));
        }

        @Test
        @DisplayName("APPOINTED + GATE_IN → GATE_IN")
        void appointed_gateIn() {
            assertEquals("GATE_IN", sm.validateAndTransition("APPOINTED", EirEvent.GATE_IN));
        }

        @Test
        @DisplayName("GATE_IN + GATE_OUT → GATE_OUT")
        void gateIn_gateOut() {
            assertEquals("GATE_OUT", sm.validateAndTransition("GATE_IN", EirEvent.GATE_OUT));
        }

        @Test
        @DisplayName("GATE_OUT + AUDIT → DONE")
        void gateOut_audit() {
            assertEquals("DONE", sm.validateAndTransition("GATE_OUT", EirEvent.AUDIT));
        }

        @Test
        @DisplayName("CREATED + CANCEL → CANCELLED")
        void created_cancel() {
            assertEquals("CANCELLED", sm.validateAndTransition("CREATED", EirEvent.CANCEL));
        }

        @Test
        @DisplayName("DISPATCHED + CANCEL → CANCELLED")
        void dispatched_cancel() {
            assertEquals("CANCELLED", sm.validateAndTransition("DISPATCHED", EirEvent.CANCEL));
        }

        @Test
        @DisplayName("APPOINTED + CANCEL → CANCELLED")
        void appointed_cancel() {
            assertEquals("CANCELLED", sm.validateAndTransition("APPOINTED", EirEvent.CANCEL));
        }

        @Test
        @DisplayName("APPOINTED + EXPIRE_APPOINT → DISPATCHED（超时释放）")
        void appointed_expireAppoint() {
            assertEquals("DISPATCHED", sm.validateAndTransition("APPOINTED", EirEvent.EXPIRE_APPOINT));
        }
    }

    @Nested
    @DisplayName("非法转移")
    class InvalidTransitions {

        @Test
        @DisplayName("CREATED + APPOINT 应抛异常（未派单不可预约）")
        void created_appoint_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("CREATED", EirEvent.APPOINT));
        }

        @Test
        @DisplayName("CREATED + GATE_IN 应抛异常（未预约不可进闸）")
        void created_gateIn_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("CREATED", EirEvent.GATE_IN));
        }

        @Test
        @DisplayName("CREATED + GATE_OUT 应抛异常")
        void created_gateOut_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("CREATED", EirEvent.GATE_OUT));
        }

        @Test
        @DisplayName("CREATED + AUDIT 应抛异常")
        void created_audit_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("CREATED", EirEvent.AUDIT));
        }

        @Test
        @DisplayName("DISPATCHED + GATE_IN 应抛异常（未预约不可进闸）")
        void dispatched_gateIn_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("DISPATCHED", EirEvent.GATE_IN));
        }

        @Test
        @DisplayName("DISPATCHED + AUDIT 应抛异常")
        void dispatched_audit_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("DISPATCHED", EirEvent.AUDIT));
        }

        @Test
        @DisplayName("APPOINTED + DISPATCH 应抛异常（不可重复派单）")
        void appointed_dispatch_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("APPOINTED", EirEvent.DISPATCH));
        }

        @Test
        @DisplayName("APPOINTED + AUDIT 应抛异常")
        void appointed_audit_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("APPOINTED", EirEvent.AUDIT));
        }

        @Test
        @DisplayName("GATE_IN + DISPATCH 应抛异常")
        void gateIn_dispatch_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("GATE_IN", EirEvent.DISPATCH));
        }

        @Test
        @DisplayName("GATE_IN + AUDIT 应抛异常（未出闸不可审单）")
        void gateIn_audit_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("GATE_IN", EirEvent.AUDIT));
        }

        @Test
        @DisplayName("GATE_IN + CANCEL 应抛异常（已进闸不可作废）")
        void gateIn_cancel_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("GATE_IN", EirEvent.CANCEL));
        }

        @Test
        @DisplayName("GATE_OUT + GATE_IN 应抛异常（不可重复进闸）")
        void gateOut_gateIn_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("GATE_OUT", EirEvent.GATE_IN));
        }

        @Test
        @DisplayName("GATE_OUT + CANCEL 应抛异常（已出闸不可作废）")
        void gateOut_cancel_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("GATE_OUT", EirEvent.CANCEL));
        }

        @Test
        @DisplayName("DISPATCHED + EXPIRE_APPOINT 应抛异常（未预约无超时概念）")
        void dispatched_expireAppoint_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("DISPATCHED", EirEvent.EXPIRE_APPOINT));
        }
    }

    @Nested
    @DisplayName("终态校验")
    class TerminalStates {

        @Test
        @DisplayName("DONE 状态不允许任何事件")
        void done_noTransitions() {
            assertAll(
                    () -> assertThrows(BusinessException.class, () ->
                            sm.validateAndTransition("DONE", EirEvent.DISPATCH)),
                    () -> assertThrows(BusinessException.class, () ->
                            sm.validateAndTransition("DONE", EirEvent.AUDIT)),
                    () -> assertThrows(BusinessException.class, () ->
                            sm.validateAndTransition("DONE", EirEvent.CANCEL))
            );
        }

        @Test
        @DisplayName("CANCELLED 状态不允许任何事件")
        void cancelled_noTransitions() {
            assertAll(
                    () -> assertThrows(BusinessException.class, () ->
                            sm.validateAndTransition("CANCELLED", EirEvent.DISPATCH)),
                    () -> assertThrows(BusinessException.class, () ->
                            sm.validateAndTransition("CANCELLED", EirEvent.CANCEL))
            );
        }

        @Test
        @DisplayName("未知状态应抛异常")
        void unknownStatus_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition("UNKNOWN", EirEvent.DISPATCH));
        }

        @Test
        @DisplayName("null 状态应抛异常")
        void nullStatus_invalid() {
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition(null, EirEvent.DISPATCH));
        }
    }

    @Nested
    @DisplayName("作废校验 canCancel")
    class CanCancel {

        @Test
        @DisplayName("CREATED 可作废")
        void created_canCancel() {
            assertTrue(sm.canCancel("CREATED"));
        }

        @Test
        @DisplayName("DISPATCHED 可作废")
        void dispatched_canCancel() {
            assertTrue(sm.canCancel("DISPATCHED"));
        }

        @Test
        @DisplayName("APPOINTED 可作废")
        void appointed_canCancel() {
            assertTrue(sm.canCancel("APPOINTED"));
        }

        @Test
        @DisplayName("GATE_IN 不可作废")
        void gateIn_cannotCancel() {
            assertFalse(sm.canCancel("GATE_IN"));
        }

        @Test
        @DisplayName("GATE_OUT 不可作废")
        void gateOut_cannotCancel() {
            assertFalse(sm.canCancel("GATE_OUT"));
        }

        @Test
        @DisplayName("DONE 不可作废")
        void done_cannotCancel() {
            assertFalse(sm.canCancel("DONE"));
        }

        @Test
        @DisplayName("CANCELLED 不可作废")
        void cancelled_cannotCancel() {
            assertFalse(sm.canCancel("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("完整业务流程")
    class FullFlow {

        @Test
        @DisplayName("正常全流程：录入→派单→预约→进闸→出闸→审单")
        void happyPath() {
            String status = "CREATED";
            status = sm.validateAndTransition(status, EirEvent.DISPATCH);
            assertEquals("DISPATCHED", status);

            status = sm.validateAndTransition(status, EirEvent.APPOINT);
            assertEquals("APPOINTED", status);

            status = sm.validateAndTransition(status, EirEvent.GATE_IN);
            assertEquals("GATE_IN", status);

            status = sm.validateAndTransition(status, EirEvent.GATE_OUT);
            assertEquals("GATE_OUT", status);

            status = sm.validateAndTransition(status, EirEvent.AUDIT);
            assertEquals("DONE", status);

            // 终态，不可再操作
            final String finalStatus = status;
            assertThrows(BusinessException.class, () ->
                    sm.validateAndTransition(finalStatus, EirEvent.DISPATCH));
        }

        @Test
        @DisplayName("派单后取消派单回到 CREATED")
        void dispatchThenCancel() {
            String status = "CREATED";
            status = sm.validateAndTransition(status, EirEvent.DISPATCH);
            assertEquals("DISPATCHED", status);

            status = sm.validateAndTransition(status, EirEvent.CANCEL_DISPATCH);
            assertEquals("CREATED", status);

            // 回到 CREATED 后可以重新派单
            status = sm.validateAndTransition(status, EirEvent.DISPATCH);
            assertEquals("DISPATCHED", status);
        }

        @Test
        @DisplayName("预约后取消预约回到 DISPATCHED")
        void appointThenCancel() {
            String status = "DISPATCHED";
            status = sm.validateAndTransition(status, EirEvent.APPOINT);
            assertEquals("APPOINTED", status);

            status = sm.validateAndTransition(status, EirEvent.CANCEL_APPOINT);
            assertEquals("DISPATCHED", status);

            // 回到 DISPATCHED 后可以重新预约
            status = sm.validateAndTransition(status, EirEvent.APPOINT);
            assertEquals("APPOINTED", status);
        }
    }
}
