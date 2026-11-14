package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.EirStatus;
import com.gss.common.PageResult;
import com.gss.dto.AppointmentDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.GateLane;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.GateLaneMapper;
import com.gss.mapper.TractorMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.AppointmentEvent;
import com.gss.statemachine.AppointmentStateMachine;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.AppointmentVO;
import com.gss.vo.LaneCapacityCellVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private final GateLaneMapper laneMapper;
    private final EirStateMachine stateMachine;
    private final AppointmentStateMachine apptStateMachine;

    /** 凭证号前缀：APT + yyyyMMdd + 6 位当日序列 */
    private static final String VOUCHER_PREFIX = "APT";
    private static final DateTimeFormatter VOUCHER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 分页查询预约。
     * driverId/companyId 由 Controller 按 JWT 身份强制注入
     */
    @Transactional(readOnly = true)
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
        // 预约前置（M1/R2）：必须先通过办单审核且完成派单；三种原因分别给文案，便于客户自助定位
        if (Constants.ST_SUBMITTED.equals(eir.getStatus())) {
            throw new BusinessException("办单申请尚未审核，暂不能预约");
        }
        if (Constants.ST_REJECTED.equals(eir.getStatus())) {
            String reason = eir.getRejectReason() == null || eir.getRejectReason().isBlank()
                    ? "未填写原因" : eir.getRejectReason();
            throw new BusinessException("办单申请已被驳回（原因：" + reason + "），暂不能预约");
        }
        if (Constants.ST_APPROVED.equals(eir.getStatus())) {
            throw new BusinessException("单证尚未派单，暂不能预约");
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

        // M3：预约必须落到启用中的车道
        GateLane lane = laneMapper.selectById(dto.getLaneId());
        if (lane == null) {
            throw new BusinessException("预约车道不存在");
        }
        if (!Constants.LANE_ENABLED.equals(lane.getStatus())) {
            throw new BusinessException("车道 " + lane.getLaneNo() + " 已停用，请选择其他车道");
        }

        Appointment appt = new Appointment();
        appt.setEirId(eir.getId());
        appt.setPlateNo(plateNo.trim());
        appt.setApptDate(dto.getApptDate());
        appt.setTimeSlot(timeSlot);
        appt.setStatus(Constants.APPT_ACTIVE);
        appt.setDriverId(driverId);
        appt.setLaneId(dto.getLaneId());
        // 凭证号由服务端生成，前端不可传入；唯一索引 + 冲突重试保证并发不重复
        appt.setVoucherNo(nextVoucherNo());

        // 时段容量控制：幂等初始化容量记录后 CAS 扣减，影响行数为 0 即已约满，防并发超约
        // M3：容量按车道隔离——初始化/扣减都带 lane_id，不同车道互不影响
        slotCapacityMapper.insertIgnore(dto.getLaneId(), dto.getApptDate(), timeSlot,
                lane.getCapacityPerSlot() == null ? Constants.DEFAULT_SLOT_CAPACITY : lane.getCapacityPerSlot());
        if (slotCapacityMapper.tryBook(dto.getLaneId(), dto.getApptDate(), timeSlot) == 0) {
            throw new BusinessException("车道 " + lane.getLaneNo() + " 该时段预约已满，请选择其他车道或时段");
        }

        insertWithVoucherRetry(appt);

        // 单证推进为已预约（CAS：仅仍为 DISPATCHED 才生效），防止并发重复预约
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
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

        // 状态机校验（ACTIVE → CANCELLED）+ CAS：仅仍为 ACTIVE 才置为已取消，
        // 防并发重复取消导致容量重复释放；Service 不直接 setStatus
        String cancelledStatus = apptStateMachine.validateAndTransition(appt.getStatus(), AppointmentEvent.CANCEL);
        int rows = appointmentMapper.updateStatusIf(id, cancelledStatus, Constants.APPT_ACTIVE);
        if (rows == 0) {
            throw new BusinessException("预约状态已被其他操作变更，请刷新后重试");
        }

        // 释放时段容量（M3：按预约所属车道归还，避免还错车道）
        if (StringUtils.hasText(appt.getTimeSlot())) {
            slotCapacityMapper.release(appt.getLaneId(), appt.getApptDate(), appt.getTimeSlot());
        }

        // 单证回退到已派单（CAS：仅仍为 APPOINTED 才生效），允许重新预约
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
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
     * 生成当日凭证号：APT + yyyyMMdd + 6 位当日序列，日重置。
     * 序列取当日最大值 +1；并发下可能撞号，由 voucher_no 唯一索引兜底，调用方重试一次。
     */
    private String nextVoucherNo() {
        String prefix = VOUCHER_PREFIX + LocalDate.now().format(VOUCHER_DATE);
        Integer max = appointmentMapper.selectMaxVoucherSeq(prefix);
        int next = (max == null ? 0 : max) + 1;
        if (next > 999999) {
            throw new BusinessException("当日预约凭证号已用尽，请联系管理员");
        }
        return prefix + String.format("%06d", next);
    }

    /**
     * 插入预约：凭证号唯一索引冲突时重新取号重试一次（并发创建场景）。
     * MySQL 的重复键错误只回滚单条语句、不中断当前事务，因此重试是安全的。
     */
    private void insertWithVoucherRetry(Appointment appt) {
        try {
            appointmentMapper.insert(appt);
        } catch (DuplicateKeyException conflict) {
            appt.setVoucherNo(nextVoucherNo());
            appointmentMapper.insert(appt);
        }
    }

    /**
     * 查询某日期「车道 × 时段」容量矩阵（M3），创建预约弹窗据此选择车道与时段。
     */
    @Transactional(readOnly = true)
    public List<LaneCapacityCellVO> slotCapacity(LocalDate date) {
        return laneMapper.selectCapacityMatrix(date);
    }
}
