package com.gss.mapper;

import com.gss.entity.GateLane;
import com.gss.vo.LaneCapacityCellVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 闸口车道 Mapper（M3）
 */
public interface GateLaneMapper {

    List<GateLane> selectAll();

    GateLane selectById(@Param("id") Long id);

    GateLane selectByLaneNo(@Param("laneNo") String laneNo);

    int insert(GateLane lane);

    int update(GateLane lane);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 该车道未完成（ACTIVE/VERIFIED）的预约数：停用前校验 */
    Long countOpenAppointments(@Param("laneId") Long laneId);

    /** 启动自检：lane_id 在 gate_lane 中找不到对应车道的预约（最多返回 limit 条 id） */
    List<Long> selectOrphanLaneAppointmentIds(@Param("limit") int limit);

    /** 启动自检：孤儿 lane_id 预约总数 */
    Long countOrphanLaneAppointments();

    /** 车道×时段容量矩阵（未配置的格子按车道默认容量返回） */
    List<LaneCapacityCellVO> selectCapacityMatrix(@Param("date") LocalDate date);
}
