package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.TractorMapper;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.EirOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 闸口办单服务：进闸办单、出闸办单、审单。
 * 进闸拦截：有效预约；出闸拦截：费用未缴清禁止放行（所有费用统一在出闸前结清）。
 */
@Service
@RequiredArgsConstructor
public class GateService {

    private final TractorMapper tractorMapper;
    private final EirOrderMapper eirMapper;
    private final AppointmentMapper appointmentMapper;
    private final FeeRecordMapper feeMapper;
    private final EirStateMachine stateMachine;

    /**
     * 进闸办单（模拟刷卡）：APPOINTED → GATE_IN。
     * 拦截规则：
     * 1. IC 卡对应拖车存在且正常；
     * 2. 该拖车上存在【已预约】状态单证；
     * 3. 单证存在有效预约。
     * 费用不在进闸时校验，统一在出闸放行前结清。
     */
    @Transactional
    public EirOrder checkin(String icCardNo) {
        Tractor tractor = getTractor(icCardNo);

        // 定位该拖车处于"已预约"状态的单证（多张时取最早创建的一张）
        EirOrder eir = findEirByTractorAndStatus(tractor.getId(), Constants.ST_APPOINTED);
        if (eir == null) {
            throw new BusinessException("该卡对应车辆没有已预约待进闸的单证，禁止进闸");
        }

        // 校验有效预约
        Appointment activeAppt = appointmentMapper.selectActiveByEirId(eir.getId());
        if (activeAppt == null) {
            throw new BusinessException("该单证无有效进港预约，禁止进闸");
        }

        // 校验通过，进闸办单（CAS：仅仍为 APPOINTED 才生效，防止重复刷卡）
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.GATE_IN);
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(LocalDateTime.now());
        casStatus(update, eir.getStatus());

        return eirMapper.selectById(eir.getId());
    }

    /**
     * 出闸办单（模拟刷卡）：GATE_IN → GATE_OUT。
     * 拦截规则：单证存在收费记录且未缴清的，禁止出闸放行
     * （还重费、提重费等所有费用统一在出闸前结清）。
     */
    @Transactional
    public EirOrder checkout(String icCardNo) {
        Tractor tractor = getTractor(icCardNo);

        EirOrder eir = findEirByTractorAndStatus(tractor.getId(), Constants.ST_GATE_IN);
        if (eir == null) {
            throw new BusinessException("该卡对应车辆没有已进闸待出闸的单证，禁止出闸办单");
        }

        // 出闸放行前必须缴清费用
        FeeRecord fee = feeMapper.selectByEirId(eir.getId());
        if (fee != null && (fee.getPayStatus() == null || fee.getPayStatus() != 1)) {
            throw new BusinessException("该单证尚有未缴清费用，禁止出闸，请通知运输公司缴费后再放行");
        }

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.GATE_OUT);
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(eir.getGateInTime());
        update.setGateOutTime(LocalDateTime.now());
        casStatus(update, eir.getStatus());

        return eirMapper.selectById(eir.getId());
    }

    /**
     * 审单（ADMIN）：GATE_OUT → DONE。
     */
    @Transactional
    public EirOrder audit(Long eirId) {
        EirOrder eir = eirMapper.selectById(eirId);
        if (eir == null) {
            throw new BusinessException("单证不存在");
        }
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.AUDIT);

        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(eir.getGateInTime());
        update.setGateOutTime(eir.getGateOutTime());
        casStatus(update, eir.getStatus());

        return eirMapper.selectById(eirId);
    }

    /* ==================== 内部方法 ==================== */

    /** 最近办单台账：按最新闸口动作时间倒序取前 5 条（进闸/出闸/审单） */
    public List<EirOrderVO> recentOps() {
        return eirMapper.selectRecentGateOps(5);
    }

    /**
     * 状态条件更新（CAS）：仅当单证当前状态等于 expectedStatus 时才落库，
     * 影响行数为 0 说明已被并发操作抢先变更（如重复刷卡），抛出冲突异常。
     */
    private void casStatus(EirOrder update, String expectedStatus) {
        update.setExpectedStatus(expectedStatus);
        int rows = eirMapper.updateStatus(update);
        if (rows == 0) {
            throw new BusinessException("单证状态已被其他操作变更，请刷新后重试");
        }
    }

    private Tractor getTractor(String icCardNo) {
        Tractor tractor = tractorMapper.selectByIcCardNo(icCardNo);
        if (tractor == null) {
            throw new BusinessException("未识别到该 IC 卡，请核对卡号");
        }
        if (tractor.getStatus() != null && tractor.getStatus() == 0) {
            throw new BusinessException("该 IC 卡已停用，请联系运输公司");
        }
        return tractor;
    }

    /**
     * 查找指定拖车、指定状态下最早创建的一张单证（条件下推到 SQL，避免全量加载后内存过滤）
     */
    private EirOrder findEirByTractorAndStatus(Long tractorId, String status) {
        return eirMapper.selectEarliestByTractorAndStatus(tractorId, status);
    }
}
