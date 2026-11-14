package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.EirStatus;
import com.gss.common.PageResult;
import com.gss.dto.GateVerifyDTO;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.GateCheckLog;
import com.gss.entity.GateLane;
import com.gss.entity.Tractor;
import com.gss.security.LoginUser;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.GateCheckLogMapper;
import com.gss.mapper.GateLaneMapper;
import com.gss.mapper.TractorMapper;
import com.gss.statemachine.AppointmentEvent;
import com.gss.statemachine.AppointmentStateMachine;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.EirOrderVO;
import com.gss.vo.GateCheckLogVO;
import com.gss.vo.VerifyResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final AppointmentStateMachine apptStateMachine;
    private final GateCheckLogMapper checkLogMapper;
    private final GateLaneMapper laneMapper;

    /** 时钟：默认系统时区；单测可注入固定时钟，验证早到/晚到窗口 */
    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

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

        // 定位该拖车处于"已预约"状态的单证（多张时取最早创建的一张，条件下推到 SQL）
        EirOrder eir = eirMapper.selectEarliestByTractorAndStatus(tractor.getId(), Constants.ST_APPOINTED);
        if (eir == null) {
            throw new BusinessException("该卡对应车辆没有已预约待进闸的单证，禁止进闸");
        }

        // 校验有效预约（必须为当天预约，防止提前/过期进闸）
        Appointment activeAppt = appointmentMapper.selectActiveByEirId(eir.getId());
        if (activeAppt == null) {
            // M2：已在闸口完成凭证核验（VERIFIED）的预约同样允许进闸；
            // 原 ACTIVE 路径行为不变，只是在未核验时多给一次兼容查询
            activeAppt = appointmentMapper.selectByEirIdAndStatus(eir.getId(), Constants.APPT_VERIFIED);
        }
        if (activeAppt == null) {
            throw new BusinessException("该单证无有效进港预约，禁止进闸");
        }
        if (!today().equals(activeAppt.getApptDate())) {
            throw new BusinessException("预约日期为 " + activeAppt.getApptDate() + "，仅当天预约可进闸");
        }

        // 校验通过，进闸办单（CAS：仅仍为 APPOINTED 才生效，防止重复刷卡）
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.GATE_IN);
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(LocalDateTime.now());
        EirStatus.casStatus(eirMapper, update, eir.getStatus());

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

        // 定位该拖车处于"已进闸"状态的单证（多张时取最早创建的一张）
        EirOrder eir = eirMapper.selectEarliestByTractorAndStatus(tractor.getId(), Constants.ST_GATE_IN);
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
        EirStatus.casStatus(eirMapper, update, eir.getStatus());

        // M2：出闸即放行——已核验的预约置 RELEASED 并写 release_time（CAS 保证只放行一次）
        Appointment verified = appointmentMapper.selectByEirIdAndStatus(eir.getId(), Constants.APPT_VERIFIED);
        if (verified != null) {
            String released = apptStateMachine.validateAndTransition(verified.getStatus(), AppointmentEvent.RELEASE);
            appointmentMapper.markReleased(verified.getId(), released, verified.getStatus(), LocalDateTime.now());
        }

        return eirMapper.selectById(eir.getId());
    }

    /**
     * 闸口凭证核验（M2/R3）：凭证号或 IC 卡至少给一个。
     * 判定顺序（先判先后判重，文案分开）：
     *   a 查不到预约 → NO_APPOINTMENT
     *   b 预约非 ACTIVE → CANCELLED / EXPIRED（已核验/已放行按重复核验处理，幂等返回 PASS）
     *   c 单证非 APPOINTED → DOC_MISMATCH
     *   d 预约日期不是今天 → EXPIRED
     *   e 不在「时段前 30 分钟 ~ 时段后 30 分钟」窗口 → EARLY / LATE
     *   f IC 卡车辆与单证派单车辆不一致 → DOC_MISMATCH
     * 通过 → 预约 ACTIVE→VERIFIED 写 verify_time，单证保持 APPOINTED。每次尝试都写 gate_check_log。
     */
    @Transactional
    public VerifyResultVO verify(GateVerifyDTO dto, LoginUser operator) {
        String voucherNo = trim(dto.getVoucherNo());
        String icCard = trim(dto.getIcCard());
        String laneCode = trim(dto.getLaneCode());
        if (voucherNo.isEmpty() && icCard.isEmpty()) {
            throw new BusinessException("请提供预约凭证号或 IC 卡号");
        }

        Tractor tractor = null;
        if (voucherNo.isEmpty()) {
            // IC 卡查不到也按 NO_APPOINTMENT 记失败留痕，不直接抛异常
            tractor = tractorMapper.selectByIcCardNo(icCard);
        }

        Appointment appt = null;
        String multiRemark = "";
        if (!voucherNo.isEmpty()) {
            appt = appointmentMapper.selectByVoucherNo(voucherNo);
        } else if (tractor != null) {
            List<Appointment> actives = appointmentMapper.selectActiveByTractorId(tractor.getId());
            if (!actives.isEmpty()) {
                // 一车多单时取「距当前时刻最近」的一条，并把情况写进备注，避免静默取最早
                appt = actives.get(0);
                if (actives.size() > 1) {
                    multiRemark = "该车有 " + actives.size() + " 条有效预约，按最近时段取 " + appt.getTimeSlot();
                }
            }
        }

        if (appt == null) {
            return fail(Constants.ABNORMAL_NO_APPOINTMENT, "未查询到预约记录",
                    voucherNo, null, null, icCard, laneCode, operator, multiRemark);
        }

        EirOrder eir = eirMapper.selectById(appt.getEirId());
        // 越权拦截：带公司归属的闸口账号只能核验本公司单证（码头端 GATE/ADMIN 的 companyId 为空，不受限）
        if (eir != null && operator.getCompanyId() != null
                && !operator.getCompanyId().equals(eir.getCompanyId())) {
            throw new BusinessException("无权核验其他公司的单证");
        }

        String effectiveVoucher = appt.getVoucherNo();
        String eirNo = eir == null ? null : eir.getEirNo();

        // b. 预约状态
        if (!Constants.APPT_ACTIVE.equals(appt.getStatus())) {
            if (Constants.APPT_VERIFIED.equals(appt.getStatus())
                    || Constants.APPT_RELEASED.equals(appt.getStatus())) {
                // 重复核验：幂等返回 PASS，不再产生放行动作
                String msg = Constants.APPT_RELEASED.equals(appt.getStatus())
                        ? "该预约已放行（重复核验）" : "该预约已核验，尚未出闸（重复核验）";
                // 单证可能已被删除/历史数据不一致：这里与下面的失败分支一样做空保护，避免 NPE
                return pass(effectiveVoucher, eir == null ? null : eir.getId(), eirNo, appt.getId(),
                        operator, icCard, laneCode, multiRemark, msg);
            }
            if (Constants.APPT_CANCELLED.equals(appt.getStatus())) {
                return fail(Constants.ABNORMAL_CANCELLED, "预约已取消，禁止进闸",
                        effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
            }
            return fail(Constants.ABNORMAL_EXPIRED, "预约已失约，禁止进闸",
                    effectiveVoucher, eir == null ? null : eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
        }

        // c. 单证状态
        if (eir == null || !Constants.ST_APPOINTED.equals(eir.getStatus())) {
            return fail(Constants.ABNORMAL_DOC_MISMATCH, "单证状态与预约不匹配，禁止进闸",
                    effectiveVoucher, eir == null ? null : eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
        }

        // d. 预约日期
        if (!today().equals(appt.getApptDate())) {
            return fail(Constants.ABNORMAL_EXPIRED, "预约日期不是今天（" + appt.getApptDate() + "），禁止进闸",
                    effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
        }

        // e. 时段窗口：开始前 30 分钟 ~ 结束后 30 分钟
        LocalTime slotStart = LocalTime.parse(appt.getTimeSlot().substring(0, 5));
        LocalTime slotEnd = LocalTime.parse(appt.getTimeSlot().substring(6, 11));
        LocalDateTime windowStart = appt.getApptDate().atTime(slotStart).minusMinutes(Constants.VERIFY_WINDOW_MINUTES);
        LocalDateTime windowEnd = appt.getApptDate().atTime(slotEnd).plusMinutes(Constants.VERIFY_WINDOW_MINUTES);
        LocalDateTime now = now();
        if (now.isBefore(windowStart)) {
            return fail(Constants.ABNORMAL_EARLY, "早到：未进入预约时段窗口（" + appt.getTimeSlot() + "）",
                    effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
        }
        if (now.isAfter(windowEnd)) {
            return fail(Constants.ABNORMAL_LATE, "晚到：已超出预约时段窗口（" + appt.getTimeSlot() + "）",
                    effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
        }

        // f0. 车道一致性（M3）：传了 laneCode 就必须与预约车道一致；不传则跳过（兼容旧调用）
        if (!laneCode.isEmpty()) {
            GateLane lane = laneMapper.selectByLaneNo(laneCode);
            if (lane == null || !lane.getId().equals(appt.getLaneId())) {
                return fail(Constants.ABNORMAL_DOC_MISMATCH, "车道与预约车道不匹配，禁止进闸",
                        effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
            }
        }

        // f. 车辆与单证派单车辆一致性（按 IC 卡核验时才有意义）
        if (!icCard.isEmpty()) {
            if (tractor == null || eir.getTractorId() == null || !eir.getTractorId().equals(tractor.getId())) {
                return fail(Constants.ABNORMAL_DOC_MISMATCH, "车辆与单证派单车辆不匹配，禁止进闸",
                        effectiveVoucher, eir.getId(), appt.getId(), icCard, laneCode, operator, multiRemark);
            }
        }

        // 通过：ACTIVE → VERIFIED + verify_time（CAS，防重复核验）
        String verifiedStatus = apptStateMachine.validateAndTransition(appt.getStatus(), AppointmentEvent.VERIFY);
        int rows = appointmentMapper.markVerified(appt.getId(), verifiedStatus, appt.getStatus(), now);
        if (rows == 0) {
            throw new BusinessException("预约状态已被其他操作变更，请刷新后重试");
        }
        return pass(effectiveVoucher, eir.getId(), eirNo, appt.getId(), operator, icCard, laneCode, multiRemark, "核验通过，允许进闸");
    }

    /**
     * 核验留痕分页（GATE / ADMIN）
     */
    @Transactional(readOnly = true)
    public PageResult<GateCheckLogVO> checkLogs(Integer page, Integer size, String result) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<GateCheckLogVO> rows = checkLogMapper.selectPage(trim(result), (p - 1) * s, s);
        return PageResult.of(checkLogMapper.countPage(trim(result)), rows);
    }

    /** 记录 PASS 留痕并返回结果（eir_id 与 appointment_id 都必须落真实的键） */
    private VerifyResultVO pass(String voucherNo, Long eirId, String eirNo, Long appointmentId, LoginUser operator,
                                String icCard, String laneCode, String multiRemark, String message) {
        writeLog(voucherNo, eirId, appointmentId,
                Constants.CHECK_PASS, null, operator, remark(icCard, laneCode, multiRemark));
        VerifyResultVO vo = new VerifyResultVO();
        vo.setResult(Constants.CHECK_PASS);
        vo.setMessage(message);
        vo.setVoucherNo(voucherNo);
        vo.setEirNo(eirNo);
        vo.setAppointmentId(appointmentId);
        return vo;
    }

    /** 记录 FAIL 留痕并返回结果 */
    private VerifyResultVO fail(String abnormalType, String message, String voucherNo, Long eirId, Long appointmentId,
                                String icCard, String laneCode, LoginUser operator, String multiRemark) {
        writeLog(voucherNo, eirId, appointmentId, Constants.CHECK_FAIL, abnormalType, operator,
                remark(icCard, laneCode, multiRemark));
        VerifyResultVO vo = new VerifyResultVO();
        vo.setResult(Constants.CHECK_FAIL);
        vo.setAbnormalType(abnormalType);
        vo.setMessage(message);
        vo.setVoucherNo(voucherNo);
        vo.setAppointmentId(appointmentId);
        return vo;
    }

    /** 写一条核验留痕（voucherNo 为空时由 appointmentId 反查补全） */
    private void writeLog(String voucherNo, Long eirId, Long appointmentId, String result, String abnormalType,
                          LoginUser operator, String remark) {
        GateCheckLog log = new GateCheckLog();
        log.setVoucherNo(voucherNo);
        log.setEirId(eirId);
        log.setAppointmentId(appointmentId);
        log.setCheckTime(now());
        log.setResult(result);
        log.setAbnormalType(abnormalType);
        log.setOperatorId(operator == null ? null : operator.getUserId());
        log.setRemark(remark);
        checkLogMapper.insert(log);
    }

    /** 备注拼接：IC 卡 / 车道 / 多预约提示 */
    private String remark(String icCard, String laneCode, String multiRemark) {
        StringBuilder sb = new StringBuilder();
        if (!icCard.isEmpty()) {
            sb.append("IC卡=").append(icCard);
        }
        if (!laneCode.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append("车道=").append(laneCode);
        }
        if (multiRemark != null && !multiRemark.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(multiRemark);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
        EirStatus.casStatus(eirMapper, update, eir.getStatus());

        return eirMapper.selectById(eirId);
    }

    /**
     * 最近办单台账：按最新闸口动作时间倒序取前 5 条（进闸/出闸/审单）
     */
    @Transactional(readOnly = true)
    public List<EirOrderVO> recentOps() {
        return eirMapper.selectRecentGateOps(5);
    }

    /**
     * 按 IC 卡号查询拖车，并拦截不存在与已停用的卡
     */
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
}
