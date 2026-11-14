package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.LaneCapacityBatchDTO;
import com.gss.dto.LaneDTO;
import com.gss.entity.GateLane;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.GateLaneMapper;
import com.gss.vo.LaneCapacityCellVO;
import com.gss.vo.LaneVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 闸口车道与配额服务（M3）。
 * 容量按车道隔离：每个车道每天每个时段一条 appt_slot_capacity 记录，
 * 唯一键 (lane_id, appt_date, time_slot)。
 */
@Service
@RequiredArgsConstructor
public class LaneService {

    private final GateLaneMapper laneMapper;
    private final ApptSlotCapacityMapper capacityMapper;

    /** 车道列表（GATE/ADMIN）：附带未完成预约数，便于停用前判断 */
    @Transactional(readOnly = true)
    public List<LaneVO> list() {
        List<GateLane> lanes = laneMapper.selectAll();
        List<LaneVO> result = new ArrayList<>(lanes.size());
        for (GateLane lane : lanes) {
            LaneVO vo = new LaneVO();
            vo.setId(lane.getId());
            vo.setGateNo(lane.getGateNo());
            vo.setLaneNo(lane.getLaneNo());
            vo.setCapacityPerSlot(lane.getCapacityPerSlot());
            vo.setStatus(lane.getStatus());
            vo.setRemark(lane.getRemark());
            vo.setActiveAppointments(laneMapper.countOpenAppointments(lane.getId()));
            vo.setCapacitySummary("默认容量 " + lane.getCapacityPerSlot() + " / 时段");
            result.add(vo);
        }
        return result;
    }

    /** 新增车道（ADMIN）：(gate_no, lane_no) 唯一，冲突给出明确提示 */
    @Transactional
    public void create(LaneDTO dto) {
        GateLane lane = new GateLane();
        lane.setGateNo(dto.getGateNo().trim());
        lane.setLaneNo(dto.getLaneNo().trim());
        lane.setCapacityPerSlot(dto.getCapacityPerSlot());
        lane.setStatus(Constants.LANE_ENABLED);
        lane.setRemark(dto.getRemark());
        try {
            laneMapper.insert(lane);
        } catch (DuplicateKeyException conflict) {
            throw new BusinessException("同一闸口下的车道号已存在：" + dto.getGateNo() + " / " + dto.getLaneNo());
        }
    }

    /** 修改车道（ADMIN） */
    @Transactional
    public void update(Long id, LaneDTO dto) {
        mustExist(id);
        GateLane lane = new GateLane();
        lane.setId(id);
        lane.setGateNo(dto.getGateNo().trim());
        lane.setLaneNo(dto.getLaneNo().trim());
        lane.setCapacityPerSlot(dto.getCapacityPerSlot());
        lane.setRemark(dto.getRemark());
        try {
            laneMapper.update(lane);
        } catch (DuplicateKeyException conflict) {
            throw new BusinessException("同一闸口下的车道号已存在：" + dto.getGateNo() + " / " + dto.getLaneNo());
        }
    }

    /**
     * 启用/停用车道（ADMIN）。
     * 停用前校验：该车道还有未完成（ACTIVE / VERIFIED）的预约时拒绝，并给出数量。
     */
    @Transactional
    public void toggle(Long id, boolean enabled) {
        GateLane lane = mustExist(id);
        if (!enabled) {
            Long open = laneMapper.countOpenAppointments(id);
            if (open != null && open > 0) {
                throw new BusinessException("车道 " + lane.getLaneNo() + " 还有 " + open + " 个未完成预约，不能停用");
            }
        }
        laneMapper.updateStatus(id, enabled ? Constants.LANE_ENABLED : Constants.LANE_DISABLED);
    }

    /** 车道 × 时段容量矩阵（含未配置格子按默认容量） */
    @Transactional(readOnly = true)
    public List<LaneCapacityCellVO> capacity(LocalDate date) {
        return laneMapper.selectCapacityMatrix(date);
    }

    /**
     * 按日期批量生成/调整容量（ADMIN）。
     * 在同一个事务里一次性 upsert，任一格子失败整体回滚。
     */
    @Transactional
    public void batchCapacity(LaneCapacityBatchDTO dto) {
        if (dto.getCells() == null || dto.getCells().isEmpty()) {
            throw new BusinessException("容量明细不能为空");
        }
        // 同一批里不允许重复配置同一个 (车道, 时段)，否则最后一条会静默覆盖前一条
        Set<String> seen = new HashSet<>();
        for (LaneCapacityBatchDTO.Cell cell : dto.getCells()) {
            String key = cell.getLaneId() + "::" + cell.getTimeSlot();
            if (!seen.add(key)) {
                throw new BusinessException("同一批里重复配置了车道 " + cell.getLaneId()
                        + " 的时段 " + cell.getTimeSlot() + "，请合并为一条");
            }
            GateLane lane = laneMapper.selectById(cell.getLaneId());
            if (lane == null) {
                throw new BusinessException("车道不存在：" + cell.getLaneId());
            }
            if (!Constants.LANE_ENABLED.equals(lane.getStatus())) {
                throw new BusinessException("车道 " + lane.getLaneNo() + " 已停用，不能配置容量");
            }
            // 容量不得低于该时段已占用量，否则会直接制造超卖（R4 的核心约束）
            Integer booked = capacityMapper.selectBookedCount(cell.getLaneId(), dto.getDate(), cell.getTimeSlot());
            int occupied = booked == null ? 0 : booked;
            if (cell.getCapacity() < occupied) {
                throw new BusinessException("车道 " + lane.getLaneNo() + " / 时段 " + cell.getTimeSlot()
                        + " 已占 " + occupied + "，容量不能低于 " + occupied);
            }
        }
        capacityMapper.batchUpsert(dto.getCells(), dto.getDate(), 0);
    }

    private GateLane mustExist(Long id) {
        GateLane lane = laneMapper.selectById(id);
        if (lane == null) {
            throw new BusinessException("车道不存在");
        }
        return lane;
    }
}
