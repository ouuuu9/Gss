package com.gss.mapper;

import com.gss.entity.RefundRecord;
import com.gss.vo.RefundRecordVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退款记录 Mapper
 */
public interface RefundRecordMapper {

    /** 新增退款申请 */
    int insert(RefundRecord record);

    /** 按ID查询 */
    RefundRecord selectById(@Param("id") Long id);

    /** 查询某单证待审批的申请 */
    RefundRecord selectPendingByEirId(@Param("eirId") Long eirId);

    /** 退款编号是否已存在 */
    int countByRefundNo(@Param("refundNo") String refundNo);

    /** 查询某收费记录已通过的退款 */
    RefundRecord selectApprovedByFeeId(@Param("feeRecordId") Long feeRecordId);

    /** 审批通过：仅 PENDING 可流转（CAS） */
    int approve(@Param("id") Long id,
                @Param("refundNo") String refundNo,
                @Param("approver") String approver);

    /** 驳回：仅 PENDING 可流转（CAS） */
    int reject(@Param("id") Long id,
               @Param("approver") String approver,
               @Param("reason") String reason);

    /** 分页查询 */
    List<RefundRecordVO> selectPage(@Param("status") String status,
                                    @Param("companyId") Long companyId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("status") String status,
                   @Param("companyId") Long companyId);
}
