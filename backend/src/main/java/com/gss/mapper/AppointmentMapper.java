package com.gss.mapper;

import com.gss.entity.Appointment;
import com.gss.vo.AppointmentVO;
import org.apache.ibatis.annotations.Param;

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

    /** 新增预约 */
    int insert(Appointment appointment);

    /** 更新预约状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 有效预约总数（看板用） */
    Long countActive();

    /** 条件更新预约状态（CAS：仅当前状态为 expectedStatus 时生效） */
    int updateStatusIf(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("expectedStatus") String expectedStatus);

    /** 查询已超时未进闸的有效预约（定时释放任务用） */
    List<Appointment> selectExpired();
}
