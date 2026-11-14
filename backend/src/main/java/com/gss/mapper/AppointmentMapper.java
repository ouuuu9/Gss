package com.gss.mapper;

import com.gss.entity.Appointment;
import com.gss.vo.AppointmentVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进港预约 Mapper
 */
public interface AppointmentMapper {

    /** 按ID查询 */
    Appointment selectById(@Param("id") Long id);

    /** 分页查询（含单证编号、司机姓名、箱号等） */
    List<AppointmentVO> selectPage(@Param("eirNo") String eirNo,
                                   @Param("driverId") Long driverId,
                                   @Param("status") String status,
                                   @Param("companyId") Long companyId,
                                   @Param("apptDate") String apptDate,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("eirNo") String eirNo,
                   @Param("driverId") Long driverId,
                   @Param("status") String status,
                   @Param("companyId") Long companyId,
                   @Param("apptDate") String apptDate);

    /** 查询某单证当前有效预约（进闸校验用） */
    Appointment selectActiveByEirId(@Param("eirId") Long eirId);

    /** 按单证 + 状态查预约（出闸时把 VERIFIED 置为 RELEASED 用） */
    Appointment selectByEirIdAndStatus(@Param("eirId") Long eirId, @Param("status") String status);

    /** 新增预约 */
    int insert(Appointment appointment);

    /** 更新预约状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 条件更新预约状态（CAS：仅当前状态为 expectedStatus 时生效） */
    int updateStatusIf(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("expectedStatus") String expectedStatus);

    /** 查询已超时未进闸的有效预约（定时释放任务用）：只取【未核验】且时段已结束的 ACTIVE */
    List<Appointment> selectExpired();

    /** 按凭证号查询预约（闸口核验用） */
    Appointment selectByVoucherNo(@Param("voucherNo") String voucherNo);

    /**
     * 按拖车（IC 卡对应车辆）查询 ACTIVE 预约，按「距当前时刻最近」排序。
     * 一辆车有多条有效预约时，调用方取第一条并记录备注，避免旧实现静默取最早的坑。
     */
    List<Appointment> selectActiveByTractorId(@Param("tractorId") Long tractorId);

    /** 当日（前缀 APTyyyyMMdd）已用的最大凭证序列；无记录返回 null */
    Integer selectMaxVoucherSeq(@Param("prefix") String prefix);

    /** 核验：ACTIVE → VERIFIED，同时写 verify_time（CAS 防重复核验） */
    int markVerified(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("verifyTime") LocalDateTime verifyTime);

    /** 放行：VERIFIED → RELEASED，同时写 release_time（CAS 保证只放行一次） */
    int markReleased(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("releaseTime") LocalDateTime releaseTime);
}
