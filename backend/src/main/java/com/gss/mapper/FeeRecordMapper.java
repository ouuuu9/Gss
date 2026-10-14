package com.gss.mapper;

import com.gss.entity.FeeRecord;
import com.gss.vo.FeeRecordVO;
import com.gss.vo.FeeReportVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收费记录 Mapper
 */
public interface FeeRecordMapper {

    /** 按ID查询 */
    FeeRecord selectById(@Param("id") Long id);

    /** 按单证ID查询收费记录 */
    FeeRecord selectByEirId(@Param("eirId") Long eirId);

    /** 按ID集合查询（批量缴费用） */
    List<FeeRecord> selectByIds(@Param("ids") List<Long> ids);

    /** 分页查询（含公司名、单证编号） */
    List<FeeRecordVO> selectPage(@Param("eirNo") String eirNo,
                                 @Param("companyId") Long companyId,
                                 @Param("payStatus") Integer payStatus,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("eirNo") String eirNo,
                   @Param("companyId") Long companyId,
                   @Param("payStatus") Integer payStatus);

    /** 新增收费记录 */
    int insert(FeeRecord feeRecord);

    /** 按单证ID删除收费记录（取消派单时回滚收费） */
    int deleteByEirId(@Param("eirId") Long eirId);

    /** 单笔缴费：更新缴费状态、时间、操作人 */
    int markPaid(@Param("id") Long id,
                 @Param("operator") String operator);

    /** 批量缴费 */
    int markPaidBatch(@Param("ids") List<Long> ids,
                      @Param("operator") String operator);

    /** 收费报表：按公司汇总缴费笔数与金额 */
    List<FeeReportVO> selectReport(@Param("companyId") Long companyId);

    /** 已缴费总金额（看板用） */
    java.math.BigDecimal sumPaidAmount();

    /** 未缴费总额 */
    java.math.BigDecimal sumUnpaidAmount();
}
