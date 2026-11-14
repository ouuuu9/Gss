package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.RefundRecord;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.RefundRecordMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.EirStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款服务单元测试（纯 Mockito，不依赖 Spring 与数据库）。
 * 覆盖：申请创建、重复申请拦截、审批原子链、进闸自动驳回、并发重复审批。
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRecordMapper refundMapper;
    @Mock
    private FeeRecordMapper feeMapper;
    @Mock
    private EirOrderMapper eirMapper;
    @Mock
    private EirCanceller canceller;

    private RefundService service;

    @BeforeEach
    void setUp() {
        service = new RefundService(refundMapper, feeMapper, eirMapper, new EirStateMachine(), canceller);
    }

    @AfterEach
    void tearDown() {
        // 本测试不依赖线程上下文，保留占位以便后续扩展
    }

    private LoginUser companyUser() {
        return LoginUser.builder().userId(1L).role("COMPANY").companyId(5L).name("张三").build();
    }

    private LoginUser admin() {
        return LoginUser.builder().userId(2L).role("ADMIN").name("管理员").build();
    }

    private EirOrder eir(long id, String status) {
        EirOrder e = new EirOrder();
        e.setId(id);
        e.setStatus(status);
        e.setCompanyId(5L);
        return e;
    }

    private FeeRecord fee(long id, int payStatus) {
        FeeRecord f = new FeeRecord();
        f.setId(id);
        f.setEirId(10L);
        f.setCompanyId(5L);
        f.setAmount(new BigDecimal("50.00"));
        f.setPayStatus(payStatus);
        return f;
    }

    private RefundRecord pending(long id) {
        RefundRecord r = new RefundRecord();
        r.setId(id);
        r.setFeeRecordId(100L);
        r.setEirId(10L);
        r.setCompanyId(5L);
        r.setAmount(new BigDecimal("50.00"));
        r.setStatus(Constants.REFUND_PENDING);
        return r;
    }

    /** 已缴费用发起退款：生成一条待审批申请，金额与收费记录一致 */
    @Test
    void applyCreatesPendingRequest() {
        when(refundMapper.selectApprovedByFeeId(100L)).thenReturn(null);
        when(refundMapper.selectPendingByEirId(10L)).thenReturn(null);

        service.apply(eir(10L, Constants.ST_DISPATCHED), fee(100L, Constants.PAY_PAID), companyUser());

        ArgumentCaptor<RefundRecord> captor = ArgumentCaptor.forClass(RefundRecord.class);
        verify(refundMapper).insert(captor.capture());
        RefundRecord saved = captor.getValue();
        assertEquals(Constants.REFUND_PENDING, saved.getStatus());
        assertEquals(100L, saved.getFeeRecordId());
        assertEquals(10L, saved.getEirId());
        assertEquals(new BigDecimal("50.00"), saved.getAmount());
        assertEquals("张三", saved.getApplicant());
    }

    /** 同一单证已有待审批申请：拒绝重复申请 */
    @Test
    void applyRejectedWhenPendingExists() {
        when(refundMapper.selectApprovedByFeeId(100L)).thenReturn(null);
        when(refundMapper.selectPendingByEirId(10L)).thenReturn(pending(1L));

        assertThrows(BusinessException.class,
                () -> service.apply(eir(10L, Constants.ST_DISPATCHED), fee(100L, Constants.PAY_PAID), companyUser()));
        verify(refundMapper, never()).insert(any());
    }

    /** 已退过的费用：拒绝再次申请 */
    @Test
    void applyRejectedWhenAlreadyRefunded() {
        when(refundMapper.selectApprovedByFeeId(100L)).thenReturn(pending(1L));

        assertThrows(BusinessException.class,
                () -> service.apply(eir(10L, Constants.ST_DISPATCHED), fee(100L, Constants.PAY_PAID), companyUser()));
        verify(refundMapper, never()).insert(any());
    }

    /** 未缴费的费用不能申请退款 */
    @Test
    void applyRejectedForUnpaidFee() {
        assertThrows(BusinessException.class,
                () -> service.apply(eir(10L, Constants.ST_DISPATCHED), fee(100L, Constants.PAY_UNPAID), companyUser()));
        verify(refundMapper, never()).insert(any());
    }

    /** 审批通过：退款记录生效 + 收费置已退 + 单证作废（同一方法内串行执行） */
    @Test
    void approveRefundsAndCancels() {
        EirOrder order = eir(10L, Constants.ST_APPOINTED);
        when(refundMapper.selectById(1L)).thenReturn(pending(1L));
        when(eirMapper.selectById(10L)).thenReturn(order);
        when(refundMapper.approve(eq(1L), anyString(), eq("管理员"))).thenReturn(1);
        when(feeMapper.markRefunded(100L, "管理员")).thenReturn(1);

        String outcome = service.approve(1L, admin());

        assertEquals("APPROVED", outcome);
        verify(feeMapper).markRefunded(100L, "管理员");
        verify(canceller).cancelNow(order);
    }

    /** 审批时单证已进闸：直接驳回并写明原因，不产生退款 */
    @Test
    void approveAutoRejectsWhenGateIn() {
        when(refundMapper.selectById(1L)).thenReturn(pending(1L));
        when(eirMapper.selectById(10L)).thenReturn(eir(10L, Constants.ST_GATE_IN));
        when(refundMapper.reject(1L, "管理员", "单证已进闸，不能作废")).thenReturn(1);

        String outcome = service.approve(1L, admin());

        assertEquals("REJECTED_GATE_IN", outcome);
        verify(refundMapper, never()).approve(any(), anyString(), anyString());
        verify(feeMapper, never()).markRefunded(any(), anyString());
        verify(canceller, never()).cancelNow(any());
    }

    /** 并发重复审批：CAS 只有一条能成功，后到的直接失败且不退款 */
    @Test
    void approveFailsWhenAlreadyHandled() {
        when(refundMapper.selectById(1L)).thenReturn(pending(1L));
        when(eirMapper.selectById(10L)).thenReturn(eir(10L, Constants.ST_APPOINTED));
        when(refundMapper.approve(eq(1L), anyString(), anyString())).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.approve(1L, admin()));
        verify(feeMapper, never()).markRefunded(any(), anyString());
        verify(canceller, never()).cancelNow(any());
    }

    /** 收费记录状态已变化（如并发退款）：审批整体失败，不作废单证 */
    @Test
    void approveFailsWhenFeeStateChanged() {
        when(refundMapper.selectById(1L)).thenReturn(pending(1L));
        when(eirMapper.selectById(10L)).thenReturn(eir(10L, Constants.ST_APPOINTED));
        when(refundMapper.approve(eq(1L), anyString(), anyString())).thenReturn(1);
        when(feeMapper.markRefunded(100L, "管理员")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.approve(1L, admin()));
        verify(canceller, never()).cancelNow(any());
    }

    /** 驳回：申请置 REJECTED */
    @Test
    void rejectMovesToRejected() {
        when(refundMapper.selectById(1L)).thenReturn(pending(1L));
        when(refundMapper.reject(1L, "管理员", "金额有误")).thenReturn(1);

        service.reject(1L, "金额有误", admin());

        verify(refundMapper).reject(1L, "管理员", "金额有误");
    }

    /** 已处理的申请不能再次驳回 */
    @Test
    void rejectFailsWhenNotPending() {
        RefundRecord done = pending(1L);
        done.setStatus(Constants.REFUND_APPROVED);
        when(refundMapper.selectById(1L)).thenReturn(done);

        assertThrows(BusinessException.class, () -> service.reject(1L, "重复驳回", admin()));
        verify(refundMapper, never()).reject(any(), anyString(), anyString());
    }
}
