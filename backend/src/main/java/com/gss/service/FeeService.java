package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.PageResult;
import com.gss.entity.FeeRecord;
import com.gss.mapper.FeeRecordMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.vo.FeeRecordVO;
import com.gss.vo.FeeReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收费服务：收费记录查询、单笔缴费、批量缴费、收费报表。
 * COMPANY 角色只能查看/缴纳本公司费用。
 */
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRecordMapper feeMapper;

    /**
     * 分页查询收费记录（companyId 由 Controller 按 JWT 强制注入）
     */
    @Transactional(readOnly = true)
    public PageResult<FeeRecordVO> page(Integer page, Integer size,
                                        String eirNo, Integer payStatus, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<FeeRecordVO> rows = feeMapper.selectPage(eirNo, companyId, payStatus, (p - 1) * s, s);
        Long total = feeMapper.countPage(eirNo, companyId, payStatus);
        return PageResult.of(total, rows);
    }

    /**
     * 单笔缴费
     */
    @Transactional
    public void pay(Long id) {
        LoginUser operator = UserContext.require();
        FeeRecord fee = mustExist(id);
        checkOwnership(fee, operator);

        if (fee.getPayStatus() != null && fee.getPayStatus() == 1) {
            throw new BusinessException("该费用已缴费，请勿重复缴费");
        }
        int rows = feeMapper.markPaid(id, operator.getName());
        if (rows == 0) {
            throw new BusinessException("缴费失败，费用可能已被处理");
        }
    }

    /**
     * 批量缴费：仅处理属于本公司且未缴费的记录
     *
     * @return 实际缴费笔数
     */
    @Transactional
    public int batchPay(List<Long> ids) {
        LoginUser operator = UserContext.require();
        List<FeeRecord> fees = feeMapper.selectByIds(ids);
        if (fees == null || fees.isEmpty()) {
            throw new BusinessException("未找到选中的收费记录");
        }

        // 过滤出本公司且未缴费的记录
        List<Long> payableIds = fees.stream()
                .filter(f -> f.getCompanyId().equals(operator.getCompanyId()))
                .filter(f -> f.getPayStatus() == null || f.getPayStatus() == 0)
                .map(FeeRecord::getId)
                .toList();

        if (payableIds.isEmpty()) {
            throw new BusinessException("所选记录均无需缴费（可能已缴费或不属于本公司）");
        }
        feeMapper.markPaidBatch(payableIds, operator.getName());
        return payableIds.size();
    }

    /**
     * 收费报表：按公司汇总笔数与金额。
     * COMPANY 仅返回本公司数据。
     */
    @Transactional(readOnly = true)
    public List<FeeReportVO> report(Long companyId) {
        return feeMapper.selectReport(companyId);
    }

    /* ==================== 内部方法 ==================== */

    private FeeRecord mustExist(Long id) {
        FeeRecord fee = feeMapper.selectById(id);
        if (fee == null) {
            throw new BusinessException("收费记录不存在");
        }
        return fee;
    }

    private void checkOwnership(FeeRecord fee, LoginUser operator) {
        if (!fee.getCompanyId().equals(operator.getCompanyId())) {
            throw new BusinessException("只能缴纳本公司的费用");
        }
    }
}
