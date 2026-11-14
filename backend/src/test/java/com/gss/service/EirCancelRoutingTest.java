package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.EirStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单证作废分流单元测试：未缴费直接作废，已缴费转退款申请。
 */
@ExtendWith(MockitoExtension.class)
class EirCancelRoutingTest {

    @Mock private EirOrderMapper eirMapper;
    @Mock private DriverMapper driverMapper;
    @Mock private TractorMapper tractorMapper;
    @Mock private TransportCompanyMapper companyMapper;
    @Mock private FeeRecordMapper feeMapper;
    @Mock private RefundService refundService;
    @Mock private EirCanceller canceller;

    private EirService service;

    @BeforeEach
    void setUp() {
        service = new EirService(eirMapper, driverMapper, tractorMapper, companyMapper,
                feeMapper, refundService, canceller, new EirStateMachine());
    }

    private LoginUser companyUser() {
        return LoginUser.builder().userId(1L).role("COMPANY").companyId(5L).name("张三").build();
    }

    private EirOrder eir(String status) {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setStatus(status);
        e.setCompanyId(5L);
        return e;
    }

    private FeeRecord fee(int payStatus) {
        FeeRecord f = new FeeRecord();
        f.setId(100L);
        f.setEirId(10L);
        f.setCompanyId(5L);
        f.setPayStatus(payStatus);
        return f;
    }

    /** 未缴费：走原逻辑直接作废 */
    @Test
    void unpaidCancelsDirectly() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPOINTED));
        when(feeMapper.selectByEirId(10L)).thenReturn(fee(Constants.PAY_UNPAID));

        String outcome = service.cancel(10L, companyUser());

        assertEquals("CANCELLED", outcome);
        verify(canceller).cancelNow(any());
        verify(refundService, never()).apply(any(), any(), any());
    }

    /** 已缴费：不直接作废，转退款申请 */
    @Test
    void paidCreatesRefundRequest() {
        EirOrder order = eir(Constants.ST_DISPATCHED);
        when(eirMapper.selectById(10L)).thenReturn(order);
        when(feeMapper.selectByEirId(10L)).thenReturn(fee(Constants.PAY_PAID));

        String outcome = service.cancel(10L, companyUser());

        assertEquals("REFUND_PENDING", outcome);
        verify(refundService).apply(order, fee(1), companyUser());
        verify(canceller, never()).cancelNow(any());
    }

    /** 已退款：拒绝重复作废 */
    @Test
    void refundedCancelRejected() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_DISPATCHED));
        when(feeMapper.selectByEirId(10L)).thenReturn(fee(Constants.PAY_REFUNDED));

        assertThrows(BusinessException.class, () -> service.cancel(10L, companyUser()));
        verify(canceller, never()).cancelNow(any());
    }

    /** 已进闸：不允许作废（保持原行为） */
    @Test
    void gateInCancelRejected() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_GATE_IN));

        assertThrows(BusinessException.class, () -> service.cancel(10L, companyUser()));
        verify(canceller, never()).cancelNow(any());
        verify(refundService, never()).apply(any(), any(), any());
    }
}
