package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.PageResult;
import com.gss.dto.AppointmentDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.TractorMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.AppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 进港预约服务。
 * 数据权限：DRIVER 只能操作本人预约；COMPANY 只能操作本公司预约。
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final ApptSlotCapacityMapper slotCapacityMapper;
    private final EirOrderMapper eirMapper;
    private final TractorMapper tractorMapper;
    private final EirStateMachine stateMachine;

    /**
     * 分页查询预约。
     * driverId/companyId 由 Controller 按 JWT 身份强制注入
     */
    public PageResult<AppointmentVO> page(Integer page, Integer size,
                                          String eirNo, String status, String apptDate,
                                          Long driverId, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<AppointmentVO> rows = appointmentMapper.selectPage(
                eirNo, driverId, status, companyId, apptDate, (p - 1) * s, s);
        Long total = appointmentMapper.countPage(eirNo, driverId, status, companyId, apptDate);
        return PageResult.of(total, rows);
    }

    /**
     * 创建预约：DISPATCHED → APPOINTED。
     * - DRIVER：只能为派给自己的单证预约，driverId 自动取当前司机
     * - COMPANY：可为派单司机代预约
     */
    @Transactional
    public void create(AppointmentDTO dto, LoginUser operator) {
        EirOrder eir = eirMapper.selectById(dto.getEirId());
        if (eir == null) {
            throw new BusinessException("单证不存在");
        }
        if (Constants.ST_CANCELLED.equals(eir.getStatus())) {
            throw new BusinessException("单证已作废，不能预约");
        }
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.APPOINT);

        // 时段合法性校验（8个两小时时段）
        String timeSlot = dto.getTimeSlot();
        if (!Arrays.asList(Constants.TIME_SLOTS).contains(timeSlot)) {
            throw new BusinessException("预约时段不合法");
        }
        if (dto.getApptDate().isBefore(LocalDate.now())) {
            throw new BusinessException("预约日期不能早于今天");
        }
        // 当天已过时段禁止预约：时段开始时间早于当前时刻即视为过期（后端兜底，与前端禁用联动）
        if (dto.getApptDate().equals(LocalDate.now())
                && LocalTime.parse(timeSlot.substring(0, 5)).isBefore(LocalTime.now())) {
            throw new BusinessException("该时段已过期，请选择其他时段");
        }

        // 确定预约司机并做数据权限校验
        Long driverId;
        if (Constants.ROLE_DRIVER.equals(operator.getRole())) {
            if (!operator.getDriverId().equals(eir.getDriverId())) {
                throw new BusinessException("只能为派给自己的单证创建预约");
            }
            driverId = operator.getDriverId();
        } else if (Constants.ROLE_COMPANY.equals(operator.getRole())) {
            if (!eir.getCompanyId().equals(operator.getCompanyId())) {
                throw new BusinessException("无权操作其他公司的单证");
            }
            driverId = dto.getDriverId() == null ? eir.getDriverId() : dto.getDriverId();
            if (!driverId.equals(eir.getDriverId())) {
                throw new BusinessException("预约司机必须是该单证的派单司机");
            }
        } else {
            throw new BusinessException("当前角色无权限创建预约");
        }

        // 车牌号：前端未传则按派单拖车自动带出
        String plateNo = dto.getPlateNo();
        if (!StringUtils.hasText(plateNo) && eir.getTractorId() != null) {
            Tractor tractor = tractorMapper.selectById(eir.getTractorId());
            if (tractor != null) {
                plateNo = tractor.getPlateNo();
            }
        }
        if (!StringUtils.hasText(plateNo)) {
            throw new BusinessException("预约车牌号不能为空");
        }

        Appointment appt = new Appointment();
        appt.setEirId(eir.getId());
        appt.setPlateNo(plateNo.trim());
        appt.setApptDate(dto.getApptDate());
        appt.setTimeSlot(timeSlot);
        appt.setStatus(Constants.APPT_ACTIVE);
        appt.setDriverId(driverId);

        // 时段容量控制：幂等初始化容量记录后 CAS 扣减，影响行数为 0 即已约满，防并发超约
        slotCapacityMapper.insertIgnore(dto.getApptDate(), timeSlot, Constants.DEFAULT_SLOT_CAPACITY);
        if (slotCapacityMapper.tryBook(dto.getApptDate(), timeSlot) == 0) {
            throw new BusinessException("该时段预约已满，请选择其他时段");
        }

        appointmentMapper.insert(appt);

        // 单证推进为已预约（CAS：仅仍为 DISPATCHED 才生效），防止并发重复预约
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        casEirStatus(update, eir.getStatus());
    }

    /**
     * 取消预约：APPOINTED → DISPATCHED（预约状态置 CANCELLED）。
     */
    @Transactional
    public void cancel(Long id, LoginUser operator) {
        Appointment appt = appointmentMapper.selectById(id);
        if (appt == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!Constants.APPT_ACTIVE.equals(appt.getStatus())) {
            throw new BusinessException("预约已取消，请勿重复操作");
        }

        EirOrder eir = eirMapper.selectById(appt.getEirId());
        if (eir == null) {
            throw new BusinessException("关联单证不存在");
        }

        // 数据权限校验
        if (Constants.ROLE_DRIVER.equals(operator.getRole())) {
            if (!operator.getDriverId().equals(appt.getDriverId())) {
                throw new BusinessException("只能取消本人的预约");
            }
        } else if (Constants.ROLE_COMPANY.equals(operator.getRole())) {
            if (!eir.getCompanyId().equals(operator.getCompanyId())) {
                throw new BusinessException("只能取消本公司的预约");
            }
        }

        // 车辆已进闸则不能取消预约
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.CANCEL_APPOINT);

        // CAS：仅仍为 ACTIVE 才置为已取消，防并发重复取消导致容量重复释放
        int rows = appointmentMapper.updateStatusIf(id, Constants.APPT_CANCELLED, Constants.APPT_ACTIVE);
        if (rows == 0) {
            throw new BusinessException("预约状态已被其他操作变更，请刷新后重试");
        }

        // 释放时段容量
        if (StringUtils.hasText(appt.getTimeSlot())) {
            slotCapacityMapper.release(appt.getApptDate(), appt.getTimeSlot());
        }

        // 单证回退到已派单（CAS：仅仍为 APPOINTED 才生效），允许重新预约
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        casEirStatus(update, eir.getStatus());
    }

    /**
     * 按单证取消预约：找到该单证的有效预约后走标准取消流程（释放容量 + 单证回退）。
     * 供司机/公司在单证页直接取消，无需先查预约 ID。
     */
    @Transactional
    public void cancelByEir(Long eirId, LoginUser operator) {
        Appointment appt = appointmentMapper.selectActiveByEirId(eirId);
        if (appt == null) {
            throw new BusinessException("该单证没有进行中的预约");
        }
        cancel(appt.getId(), operator);
    }

    /**
     * 查询某日期各时段剩余容量（未初始化的时段返回默认容量），创建预约弹窗展示约满情况
     */
    public List<Map<String, Object>> slotCapacity(String date) {
        return slotCapacityMapper.selectRemain(date, Arrays.asList(Constants.TIME_SLOTS),
                Constants.DEFAULT_SLOT_CAPACITY);
    }

    /**
     * 单证状态条件更新（CAS），影响行数为 0 时抛出并发冲突异常并回滚事务
     */
    private void casEirStatus(EirOrder update, String expectedStatus) {
        update.setExpectedStatus(expectedStatus);
        int rows = eirMapper.updateStatus(update);
        if (rows == 0) {
            throw new BusinessException("单证状态已被其他操作变更，请刷新后重试");
        }
    }
}
