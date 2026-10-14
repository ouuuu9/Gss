package com.gss.mapper;

import com.gss.entity.EirOrder;
import com.gss.vo.EirOrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * EIR单证 Mapper
 */
public interface EirOrderMapper {

    /** 按ID查询 */
    EirOrder selectById(@Param("id") Long id);

    /** 按单证编号查询 */
    EirOrder selectByEirNo(@Param("eirNo") String eirNo);

    /** 分页查询（含公司名、司机名、车牌、缴费状态） */
    List<EirOrderVO> selectPage(@Param("eirNo") String eirNo,
                                @Param("status") String status,
                                @Param("businessType") String businessType,
                                @Param("companyId") Long companyId,
                                @Param("driverId") Long driverId,
                                @Param("offset") int offset,
                                @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("eirNo") String eirNo,
                   @Param("status") String status,
                   @Param("businessType") String businessType,
                   @Param("companyId") Long companyId,
                   @Param("driverId") Long driverId);

    /** 录入单证 */
    int insert(EirOrder eirOrder);

    /** 修改单证（基本信息） */
    int update(EirOrder eirOrder);

    /** 更新单证状态及派单信息 */
    int updateStatus(EirOrder eirOrder);

    /** 删除单证 */
    int deleteById(@Param("id") Long id);

    /** 按状态统计数量（companyId 为空时统计全部） */
    Long countByStatus(@Param("status") String status,
                       @Param("companyId") Long companyId);

    /** 按拖车ID查询全部单证（闸口刷卡定位单证用） */
    List<EirOrder> selectByTractorId(@Param("tractorId") Long tractorId);

    /** 按拖车ID + 状态查询最早的一张单证（闸口刷卡定位用，条件下推 SQL） */
    EirOrder selectEarliestByTractorAndStatus(@Param("tractorId") Long tractorId,
                                              @Param("status") String status);

    /** 单证总数（companyId 为空时统计全部） */
    Long countAll(@Param("companyId") Long companyId);

    /** 按状态和日期统计单证数量 */
    Long countByStatusOnDate(@Param("status") String status, @Param("date") String date);

    /** 按业务类型统计单证数量 */
    List<Map<String, Object>> countByBusinessType();

    /** 统计最近7天每日进闸/出闸数量 */
    List<Map<String, Object>> countDailyGateFlow(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 最近闸口办单台账：按最新闸口动作（进闸/出闸/审单）时间倒序取前 limit 条 */
    List<EirOrderVO> selectRecentGateOps(@Param("limit") int limit);
}
