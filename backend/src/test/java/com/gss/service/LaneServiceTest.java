package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.dto.LaneCapacityBatchDTO;
import com.gss.dto.LaneDTO;
import com.gss.entity.GateLane;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.GateLaneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3 车道服务测试（验收 1、2）：
 * 停用有未完成预约的车道要拒绝并给数量；批量配置容量的前置校验；
 * 唯一键冲突转业务提示。
 */
@ExtendWith(MockitoExtension.class)
class LaneServiceTest {

    @Mock private GateLaneMapper laneMapper;
    @Mock private ApptSlotCapacityMapper capacityMapper;

    private LaneService service;

    @BeforeEach
    void setUp() {
        service = new LaneService(laneMapper, capacityMapper);
    }

    private GateLane lane(Long id, String status) {
        GateLane l = new GateLane();
        l.setId(id);
        l.setGateNo("GATE-1");
        l.setLaneNo("LANE-" + id);
        l.setCapacityPerSlot(10);
        l.setStatus(status);
        return l;
    }

    private LaneDTO dto() {
        LaneDTO d = new LaneDTO();
        d.setGateNo("GATE-2");
        d.setLaneNo("LANE-9");
        d.setCapacityPerSlot(12);
        return d;
    }

    /** 验收 1：停用有未完成预约的车道被拒绝，并提示数量 */
    @Test
    void disableRejectedWhenOpenAppointments() {
        when(laneMapper.selectById(2L)).thenReturn(lane(2L, Constants.LANE_ENABLED));
        when(laneMapper.countOpenAppointments(2L)).thenReturn(3L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.toggle(2L, false));

        assertTrue(error.getMessage().contains("3"));
        verify(laneMapper, never()).updateStatus(anyLong(), any());
    }

    /** 启用不受预约数限制 */
    @Test
    void enableAlwaysAllowed() {
        when(laneMapper.selectById(2L)).thenReturn(lane(2L, Constants.LANE_DISABLED));

        service.toggle(2L, true);

        verify(laneMapper).updateStatus(2L, Constants.LANE_ENABLED);
    }

    /** (gate_no, lane_no) 唯一冲突 → 业务提示 */
    @Test
    void duplicateLaneRejected() {
        when(laneMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_gate_lane"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto()));

        assertTrue(error.getMessage().contains("已存在"));
    }

    /** 验收 2：批量配置里车道不存在 → 整批拒绝，不落库 */
    @Test
    void batchRejectsMissingLane() {
        LaneCapacityBatchDTO dto = new LaneCapacityBatchDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        LaneCapacityBatchDTO.Cell cell = new LaneCapacityBatchDTO.Cell();
        cell.setLaneId(99L);
        cell.setTimeSlot("08:00-10:00");
        cell.setCapacity(10);
        dto.setCells(List.of(cell));
        when(laneMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.batchCapacity(dto));
        verify(capacityMapper, never()).batchUpsert(any(), any(), anyInt());
    }

    /** 停用车道的格子不允许配置容量 */
    @Test
    void batchRejectsDisabledLane() {
        LaneCapacityBatchDTO dto = new LaneCapacityBatchDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        LaneCapacityBatchDTO.Cell cell = new LaneCapacityBatchDTO.Cell();
        cell.setLaneId(2L);
        cell.setTimeSlot("08:00-10:00");
        cell.setCapacity(10);
        dto.setCells(List.of(cell));
        when(laneMapper.selectById(2L)).thenReturn(lane(2L, Constants.LANE_DISABLED));

        assertThrows(BusinessException.class, () -> service.batchCapacity(dto));
        verify(capacityMapper, never()).batchUpsert(any(), any(), anyInt());
    }

    /** 评审阻塞项：已占 8 时把容量调到 3 → 拒绝整批，不落库 */
    @Test
    void capacityBelowBookedRejected() {
        LaneCapacityBatchDTO dto = new LaneCapacityBatchDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        LaneCapacityBatchDTO.Cell cell = new LaneCapacityBatchDTO.Cell();
        cell.setLaneId(1L);
        cell.setTimeSlot("08:00-10:00");
        cell.setCapacity(3);
        dto.setCells(List.of(cell));
        when(laneMapper.selectById(1L)).thenReturn(lane(1L, Constants.LANE_ENABLED));
        when(capacityMapper.selectBookedCount(1L, dto.getDate(), "08:00-10:00")).thenReturn(8);

        BusinessException error = assertThrows(BusinessException.class, () -> service.batchCapacity(dto));

        assertTrue(error.getMessage().contains("已占 8"));
        assertTrue(error.getMessage().contains("容量不能低于 8"));
        verify(capacityMapper, never()).batchUpsert(any(), any(), anyInt());
    }

    /** 同一批里重复的 (车道, 时段) 直接拒绝 */
    @Test
    void duplicateCellRejected() {
        LaneCapacityBatchDTO dto = new LaneCapacityBatchDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        LaneCapacityBatchDTO.Cell a = new LaneCapacityBatchDTO.Cell();
        a.setLaneId(1L);
        a.setTimeSlot("08:00-10:00");
        a.setCapacity(10);
        LaneCapacityBatchDTO.Cell b = new LaneCapacityBatchDTO.Cell();
        b.setLaneId(1L);
        b.setTimeSlot("08:00-10:00");
        b.setCapacity(12);
        dto.setCells(List.of(a, b));
        when(laneMapper.selectById(1L)).thenReturn(lane(1L, Constants.LANE_ENABLED));
        when(capacityMapper.selectBookedCount(1L, dto.getDate(), "08:00-10:00")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.batchCapacity(dto));

        assertTrue(error.getMessage().contains("重复配置"));
        verify(capacityMapper, never()).batchUpsert(any(), any(), anyInt());
    }

    /** 正常批量配置：一次 upsert（事务由 @Transactional 保证整体回滚） */
    @Test
    void batchUpsertDelegated() {
        LaneCapacityBatchDTO dto = new LaneCapacityBatchDTO();
        dto.setDate(LocalDate.now().plusDays(1));
        LaneCapacityBatchDTO.Cell cell = new LaneCapacityBatchDTO.Cell();
        cell.setLaneId(1L);
        cell.setTimeSlot("08:00-10:00");
        cell.setCapacity(15);
        dto.setCells(List.of(cell));
        when(laneMapper.selectById(1L)).thenReturn(lane(1L, Constants.LANE_ENABLED));
        when(capacityMapper.selectBookedCount(1L, dto.getDate(), "08:00-10:00")).thenReturn(2);

        service.batchCapacity(dto);

        verify(capacityMapper).batchUpsert(dto.getCells(), dto.getDate(), 0);
    }
}