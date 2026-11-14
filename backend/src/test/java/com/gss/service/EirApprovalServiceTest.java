package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.EirApplyDTO;
import com.gss.dto.EirRejectDTO;
import com.gss.entity.EirOrder;
import com.gss.entity.TransportCompany;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M1 办单审核闭环单元测试（R1/R2）：
 * 客户提交 → 管理员通过/驳回 → 重新提交 → 派单前置校验。
 */
@ExtendWith(MockitoExtension.class)
class EirApprovalServiceTest {

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
        return LoginUser.builder().userId(2L).role("COMPANY").companyId(5L).name("运输公司甲").build();
    }

    private LoginUser adminUser() {
        return LoginUser.builder().userId(1L).role("ADMIN").name("码头管理员").build();
    }

    private EirOrder eir(String status) {
        EirOrder e = new EirOrder();
        e.setId(10L);
        e.setStatus(status);
        e.setCompanyId(5L);
        e.setBusinessType("PI-E");
        e.setContainerNo("CSNU1000009");
        e.setContainerSize(20);
        e.setContainerType("GP");
        return e;
    }

    private EirApplyDTO applyDto() {
        EirApplyDTO dto = new EirApplyDTO();
        dto.setBusinessType("PI-E");
        dto.setContainerNo("CSNU1000009");
        dto.setContainerSize(20);
        dto.setContainerType("GP");
        dto.setRemark("测试申请");
        return dto;
    }

    private EirRejectDTO rejectDto(String reason) {
        EirRejectDTO dto = new EirRejectDTO();
        dto.setRejectReason(reason);
        return dto;
    }

    /** 验收 1：客户提交 → SUBMITTED，apply_by / apply_time 落库；公司强制取登录身份 */
    @Test
    void applyCreatesSubmittedWithApplicant() {
        when(companyMapper.selectById(5L)).thenReturn(new TransportCompany());
        when(eirMapper.selectByEirNo(anyString())).thenReturn(null);

        service.apply(applyDto(), companyUser());

        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).insert(captor.capture());
        EirOrder saved = captor.getValue();
        assertEquals(Constants.ST_SUBMITTED, saved.getStatus());
        assertEquals(Constants.REVIEW_PENDING, saved.getReviewStatus());
        assertEquals(2L, saved.getApplyBy());
        assertNotNull(saved.getApplyTime());
        // 作业公司必须是登录用户所属公司，不接受前端传值
        assertEquals(5L, saved.getCompanyId());
    }

    /** 验收 2：管理员通过 → APPROVED，审核人/审核时间落库 */
    @Test
    void approveWritesReviewerAndTime() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_SUBMITTED));
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.approve(10L, adminUser());

        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).updateStatus(captor.capture());
        EirOrder update = captor.getValue();
        assertEquals(Constants.ST_APPROVED, update.getStatus());
        assertEquals(Constants.REVIEW_APPROVED, update.getReviewStatus());
        assertEquals(1L, update.getReviewBy());
        assertNotNull(update.getReviewTime());
        assertEquals(Constants.ST_SUBMITTED, update.getExpectedStatus());
    }

    /** 验收 3：驳回 → REJECTED，原因落库，并在同事务内取消预约、回补名额 */
    @Test
    void rejectCancelsAppointmentAndWritesReason() {
        EirOrder order = eir(Constants.ST_SUBMITTED);
        when(eirMapper.selectById(10L)).thenReturn(order);
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.reject(10L, rejectDto("箱号与单据不一致"), adminUser());

        // 同事务取消有效预约 + 回补 booked_count
        verify(canceller).releaseActiveAppointment(order);

        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).updateStatus(captor.capture());
        EirOrder update = captor.getValue();
        assertEquals(Constants.ST_REJECTED, update.getStatus());
        assertEquals(Constants.REVIEW_REJECTED, update.getReviewStatus());
        assertEquals("箱号与单据不一致", update.getRejectReason());
    }

    /** 驳回原因为空：直接拒绝，不触碰预约与状态 */
    @Test
    void rejectRequiresReason() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_SUBMITTED));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reject(10L, rejectDto("   "), adminUser()));

        assertTrue(error.getMessage().contains("驳回原因不能为空"));
        verify(canceller, never()).releaseActiveAppointment(any());
        verify(eirMapper, never()).updateStatus(any());
    }

    /** 驳回原因超长：拒绝 */
    @Test
    void rejectReasonTooLong() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_SUBMITTED));

        String longReason = "x".repeat(201);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reject(10L, rejectDto(longReason), adminUser()));

        assertTrue(error.getMessage().contains("不能超过 200 字"));
        verify(canceller, never()).releaseActiveAppointment(any());
    }

    /** 验收 5：驳回后重新提交 → SUBMITTED，申请信息覆盖、审核信息清空（SQL 侧 updateSubmit 负责） */
    @Test
    void resubmitResetsReviewFields() {
        EirOrder order = eir(Constants.ST_REJECTED);
        order.setRejectReason("箱号与单据不一致");
        when(eirMapper.selectById(10L)).thenReturn(order);
        when(eirMapper.updateSubmit(any())).thenReturn(1);

        service.resubmit(10L, companyUser());

        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).updateSubmit(captor.capture());
        EirOrder update = captor.getValue();
        assertEquals(Constants.ST_SUBMITTED, update.getStatus());
        assertEquals(2L, update.getApplyBy());
        assertEquals(Constants.ST_REJECTED, update.getExpectedStatus());
        // 重新提交时不在 Java 侧传原因，由 updateSubmit 的 SQL 统一清空
        assertNull(update.getRejectReason());
    }

    /** 验收 4（未审核分支）：待审核单证派单给出明确文案 */
    @Test
    void dispatchRejectedWhenPendingReview() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_SUBMITTED));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.dispatch(10L, new com.gss.dto.DispatchDTO(), companyUser()));

        assertTrue(error.getMessage().contains("尚未审核通过"));
        verify(eirMapper, never()).updateStatus(any());
    }

    /** 验收 4（已驳回分支）：驳回原因带回提示，便于客户修改后重提 */
    @Test
    void dispatchRejectedWhenReviewRejected() {
        EirOrder order = eir(Constants.ST_REJECTED);
        order.setRejectReason("箱号与单据不一致");
        when(eirMapper.selectById(10L)).thenReturn(order);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.dispatch(10L, new com.gss.dto.DispatchDTO(), companyUser()));

        assertTrue(error.getMessage().contains("已被驳回"));
        assertTrue(error.getMessage().contains("箱号与单据不一致"));
        verify(eirMapper, never()).updateStatus(any());
    }

    /** 撤销审核（APPROVED，无派单）：直接落 REJECTED，不触碰预约 */
    @Test
    void revokeFromApprovedDoesNotTouchAppointment() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_APPROVED));
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.revokeReview(10L, rejectDto("资料复核不通过"), adminUser());

        verify(canceller, never()).releaseActiveAppointment(any());
        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).updateStatus(captor.capture());
        EirOrder update = captor.getValue();
        assertEquals(Constants.ST_REJECTED, update.getStatus());
        assertEquals(Constants.REVIEW_REJECTED, update.getReviewStatus());
        assertEquals("资料复核不通过", update.getRejectReason());
        assertEquals(1L, update.getReviewBy());
        assertNull(update.getTractorId());
        assertNull(update.getDriverId());
        assertEquals(Constants.ST_APPROVED, update.getExpectedStatus());
    }

    /**
     * 撤销审核（APPOINTED）：这是验收标准 3 的「活路径」——
     * 先取消有效预约并回补名额，再清派单信息，最后一次 CAS 落 REJECTED。
     */
    @Test
    void revokeFromAppointedReleasesAppointment() {
        EirOrder order = eir(Constants.ST_APPOINTED);
        order.setTractorId(7L);
        order.setDriverId(8L);
        when(eirMapper.selectById(10L)).thenReturn(order);
        when(eirMapper.updateStatus(any())).thenReturn(1);

        service.revokeReview(10L, rejectDto("车辆信息有误"), adminUser());

        verify(canceller).releaseActiveAppointment(order);
        ArgumentCaptor<EirOrder> captor = ArgumentCaptor.forClass(EirOrder.class);
        verify(eirMapper).updateStatus(captor.capture());
        EirOrder update = captor.getValue();
        assertEquals(Constants.ST_REJECTED, update.getStatus());
        assertNull(update.getTractorId());
        assertNull(update.getDriverId());
        assertEquals(Constants.ST_APPOINTED, update.getExpectedStatus());
    }

    /** 撤销审核（已进闸）：不允许，且不触碰预约、不改状态 */
    @Test
    void revokeAfterGateInRejected() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_GATE_IN));

        assertThrows(BusinessException.class,
                () -> service.revokeReview(10L, rejectDto("x"), adminUser()));

        verify(canceller, never()).releaseActiveAppointment(any());
        verify(eirMapper, never()).updateStatus(any());
    }

    /** 撤销原因为空：拒绝 */
    @Test
    void revokeRequiresReason() {
        when(eirMapper.selectById(10L)).thenReturn(eir(Constants.ST_DISPATCHED));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.revokeReview(10L, rejectDto(" "), adminUser()));

        assertTrue(error.getMessage().contains("撤销原因不能为空"));
        verify(canceller, never()).releaseActiveAppointment(any());
        verify(eirMapper, never()).updateStatus(any());
    }

    /** 审核待办分页：总数来自 countReviewPending，偏移量按页码换算 */
    @Test
    void reviewPageUsesPendingCount() {
        when(eirMapper.selectReviewPage(0, 10)).thenReturn(List.of());
        when(eirMapper.countReviewPending()).thenReturn(3L);

        var page = service.reviewPage(1, 10);

        assertEquals(3L, page.getTotal());
        assertTrue(page.getRows().isEmpty());
        verify(eirMapper, never()).updateStatus(any());
        verify(eirMapper, never()).insert(any());
        verify(eirMapper, never()).updateSubmit(any());
        verify(eirMapper, never()).update(any());
        verify(eirMapper, never()).updateStatus(any());
        verify(eirMapper, never()).selectById(anyLong());
    }
}