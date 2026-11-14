package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.PageResult;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.RefundRecord;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.RefundRecordMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.RefundRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款服务：已缴费单证作废走「申请 → ADMIN 审批 → 退款 + 作废」闭环。
 * 退款与作废在同一事务内完成，任一步失败整体回滚。
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final DateTimeFormatter REFUND_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundRecordMapper refundMapper;
    private final FeeRecordMapper feeMapper;
    private final EirOrderMapper eirMapper;
    private final EirStateMachine stateMachine;
    private final EirCanceller canceller;

    /**
     * 发起退款申请（由 EirService.cancel 在检测到费用已缴时调用）。
     * 单证保持原状态，审批通过前不置为已作废。
     */
    @Transactional
    public void apply(EirOrder eir, FeeRecord fee, LoginUser operator) {
        if (fee.getPayStatus() == null || fee.getPayStatus() != Constants.PAY_PAID) {
            throw new BusinessException("仅已缴费的费用可以申请退款");
        }
        if (refundMapper.selectApprovedByFeeId(fee.getId()) != null) {
            throw new BusinessException("该费用已退款，请勿重复申请");
        }
        if (refundMapper.selectPendingByEirId(eir.getId()) != null) {
            throw new BusinessException("该单证已有待审批的退款申请，请等待审批");
        }

        RefundRecord record = new RefundRecord();
        record.setFeeRecordId(fee.getId());
        record.setEirId(eir.getId());
        record.setCompanyId(fee.getCompanyId());
        record.setAmount(fee.getAmount());
        record.setStatus(Constants.REFUND_PENDING);
        record.setApplicant(operator.getName());
        try {
            refundMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发下已有同一单证的待审批申请
            throw new BusinessException("该单证已有待审批的退款申请，请等待审批");
        }
    }

    /**
     * 审批通过：退款记录生效 → 收费置已退 → 单证作废（同一事务，原子）。
     * 若单证已进闸等不可作废状态，直接把申请置为驳回并写明原因。
     *
     * @return APPROVED 已退款并作废；REJECTED_GATE_IN 已自动驳回
     */
    @Transactional
    public String approve(Long id, LoginUser operator) {
        RefundRecord record = mustExist(id);
        if (!Constants.REFUND_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }

        EirOrder eir = eirMapper.selectById(record.getEirId());
        if (eir == null) {
            throw new BusinessException("关联单证不存在，无法退款");
        }
        if (!stateMachine.canCancel(eir.getStatus())) {
            int rows = refundMapper.reject(id, operator.getName(), "单证已进闸，不能作废");
            if (rows == 0) {
                throw new BusinessException("该申请已被其他操作处理，请刷新后重试");
            }
            return "REJECTED_GATE_IN";
        }

        int rows = refundMapper.approve(id, generateRefundNo(), operator.getName());
        if (rows == 0) {
            throw new BusinessException("该申请已被其他操作处理，请刷新后重试");
        }
        int feeRows = feeMapper.markRefunded(record.getFeeRecordId(), operator.getName());
        if (feeRows == 0) {
            throw new BusinessException("收费记录状态已变更，退款失败");
        }
        canceller.cancelNow(eir);
        return "APPROVED";
    }

    /**
     * 驳回退款申请（单证保持原状态）。
     */
    @Transactional
    public void reject(Long id, String reason, LoginUser operator) {
        RefundRecord record = mustExist(id);
        if (!Constants.REFUND_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该申请已处理，请勿重复操作");
        }
        int rows = refundMapper.reject(id, operator.getName(), reason);
        if (rows == 0) {
            throw new BusinessException("该申请已被其他操作处理，请刷新后重试");
        }
    }

    /**
     * 分页查询退款记录（COMPANY 仅本公司，companyId 由 Controller 强制注入）
     */
    @Transactional(readOnly = true)
    public PageResult<RefundRecordVO> page(Integer page, Integer size, String status, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<RefundRecordVO> rows = refundMapper.selectPage(status, companyId, (p - 1) * s, s);
        Long total = refundMapper.countPage(status, companyId);
        return PageResult.of(total, rows);
    }

    private RefundRecord mustExist(Long id) {
        RefundRecord record = refundMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("退款申请不存在");
        }
        return record;
    }

    /**
     * 生成退款编号：RF + yyyyMMddHHmmss + 3 位随机数
     */
    private String generateRefundNo() {
        String no;
        do {
            no = "RF" + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        } while (refundMapper.countByRefundNo(no) > 0);
        return no;
    }
}
