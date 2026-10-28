package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.EirStatus;
import com.gss.common.PageResult;
import com.gss.dto.BatchDispatchDTO;
import com.gss.dto.DispatchDTO;
import com.gss.dto.EirDTO;
import com.gss.entity.Appointment;
import com.gss.entity.Driver;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.Tractor;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import com.gss.vo.EirOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * EIR 单证服务。
 * 所有写操作在 Service 层校验前置状态，禁止状态越级流转；
 * COMPANY 角色的数据归属在此一并校验。
 */
@Service
@RequiredArgsConstructor
public class EirService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EirOrderMapper eirMapper;
    private final DriverMapper driverMapper;
    private final TractorMapper tractorMapper;
    private final TransportCompanyMapper companyMapper;
    private final FeeRecordMapper feeMapper;
    private final AppointmentMapper appointmentMapper;
    private final ApptSlotCapacityMapper slotCapacityMapper;
    private final EirStateMachine stateMachine;

    @Value("${gss.fee.amount}")
    private BigDecimal feeAmount;

    @Value("${gss.fee.pickup-amount}")
    private BigDecimal pickupFeeAmount;

    /**
     * 自注入代理：批量派单中逐条调用 dispatch 必须走 Spring 代理，
     * 才能让每条派单拥有独立事务（单条失败回滚不影响整批）
     */
    @Lazy
    @Autowired
    private EirService self;

    /**
     * 分页查询单证。
     * companyId 由 Controller 根据 JWT 身份传入（COMPANY 强制本公司）
     */
    @Transactional(readOnly = true)
    public PageResult<EirOrderVO> page(Integer page, Integer size,
                                       String eirNo, String status, String businessType,
                                       Long companyId, Long driverId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<EirOrderVO> rows = eirMapper.selectPage(eirNo, status, businessType, companyId, driverId, (p - 1) * s, s);
        Long total = eirMapper.countPage(eirNo, status, businessType, companyId, driverId);
        return PageResult.of(total, rows);
    }

    /**
     * 单证详情。
     * 数据权限：ADMIN/GATE 可查看全部；COMPANY 仅本公司；DRIVER 仅派给自己的单证，
     * 防止通过遍历 ID 水平越权查看其他公司/司机的数据。
     */
    @Transactional(readOnly = true)
    public EirOrder getById(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        if (Constants.ROLE_COMPANY.equals(operator.getRole())) {
            if (!eir.getCompanyId().equals(operator.getCompanyId())) {
                throw new BusinessException("无权查看其他公司的单证");
            }
        } else if (Constants.ROLE_DRIVER.equals(operator.getRole())) {
            if (operator.getDriverId() == null || !operator.getDriverId().equals(eir.getDriverId())) {
                throw new BusinessException("无权查看该单证");
            }
        }
        return eir;
    }

    /**
     * 录入单证（ADMIN）：初始状态 CREATED
     */
    @Transactional
    public void create(EirDTO dto) {
        validateBusiness(dto);

        EirOrder eir = new EirOrder();
        eir.setEirNo(generateEirNo(dto.getEirNo()));
        eir.setBusinessType(dto.getBusinessType());
        eir.setContainerNo(dto.getContainerNo());
        eir.setContainerSize(dto.getContainerSize());
        eir.setContainerType(dto.getContainerType());
        eir.setCompanyId(dto.getCompanyId());
        eir.setStatus(Constants.ST_CREATED);
        eir.setRemark(dto.getRemark());
        eirMapper.insert(eir);
    }

    /**
     * 修改单证：仅允许在 CREATED / DISPATCHED 状态修改基础信息，
     * 已进入闸口作业的单证禁止修改
     */
    @Transactional
    public void update(Long id, EirDTO dto) {
        EirOrder eir = mustExist(id);
        if (!Arrays.asList(Constants.ST_CREATED, Constants.ST_DISPATCHED).contains(eir.getStatus())) {
            throw new BusinessException("当前状态不允许修改单证");
        }
        validateBusiness(dto);
        eir.setBusinessType(dto.getBusinessType());
        eir.setContainerNo(dto.getContainerNo());
        eir.setContainerSize(dto.getContainerSize());
        eir.setContainerType(dto.getContainerType());
        eir.setCompanyId(dto.getCompanyId());
        eir.setRemark(dto.getRemark());
        eirMapper.update(eir);
    }

    /**
     * 作废单证。
     * 仅允许在 CREATED/DISPATCHED/APPOINTED 状态作废（车辆尚未进闸）；
     * 同时取消有效预约。已作废单证不可重复作废。
     */
    @Transactional
    public void cancel(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        checkCompanyOwnership(eir, operator);

        if (Constants.ST_CANCELLED.equals(eir.getStatus())) {
            throw new BusinessException("单证已作废，请勿重复操作");
        }
        if (!stateMachine.canCancel(eir.getStatus())) {
            throw new BusinessException("车辆已进闸作业，单证不能作废");
        }

        // 取消有效预约：CAS 置为已取消成功后归还时段名额（防止与预约取消并发导致名额重复释放）
        Appointment activeAppt = appointmentMapper.selectActiveByEirId(id);
        if (activeAppt != null) {
            int rows = appointmentMapper.updateStatusIf(activeAppt.getId(),
                    Constants.APPT_CANCELLED, Constants.APPT_ACTIVE);
            if (rows == 1 && StringUtils.hasText(activeAppt.getTimeSlot())) {
                slotCapacityMapper.release(activeAppt.getApptDate(), activeAppt.getTimeSlot());
            }
        }

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(Constants.ST_CANCELLED);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        // CAS：仅当前状态与读取时一致才落库，与派单/进闸并发时作废失败并回滚
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
    }

    /**
     * 派单（COMPANY）：CREATED → DISPATCHED。
     * 校验司机/拖车属于单证作业公司；
     * 计费规则：GR-F 还重单生成还重费，
     * PI-F 提重单生成提重费，其余类型不收费；所有费用统一在出闸前缴清。
     */
    @Transactional
    public void dispatch(Long id, DispatchDTO dto, LoginUser operator) {
        EirOrder eir = mustExist(id);
        checkCompanyOwnership(eir, operator);

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.DISPATCH);

        Driver driver = driverMapper.selectById(dto.getDriverId());
        if (driver == null || !driver.getCompanyId().equals(eir.getCompanyId()) || driver.getStatus() != 1) {
            throw new BusinessException("所选司机不存在、已停用或不属于该单证作业公司");
        }
        Tractor tractor = tractorMapper.selectById(dto.getTractorId());
        if (tractor == null || !tractor.getCompanyId().equals(eir.getCompanyId()) || tractor.getStatus() != 1) {
            throw new BusinessException("所选拖车不存在、已停用或不属于该单证作业公司");
        }

        // CAS 更新单证为已派单（仅当前仍为 CREATED 才生效），防止并发重复派单导致重复计费
        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setDriverId(driver.getId());
        update.setTractorId(tractor.getId());
        EirStatus.casStatus(eirMapper, update, eir.getStatus());

        // 还重单收还重费；提重单收提重费；均统一在出闸前缴清
        BigDecimal amount = null;
        if (Constants.BIZ_GR_F.equals(eir.getBusinessType())) {
            amount = feeAmount;
        } else if (Constants.BIZ_PI_F.equals(eir.getBusinessType())) {
            amount = pickupFeeAmount;
        }
        if (amount != null) {
            FeeRecord fee = new FeeRecord();
            fee.setEirId(id);
            fee.setPlateNo(tractor.getPlateNo());
            fee.setCompanyId(eir.getCompanyId());
            fee.setAmount(amount);
            fee.setPayStatus(0);
            feeMapper.insert(fee);
        }
    }

    /**
     * 批量派单（COMPANY）：多单证派给同一司机与拖车。
     * 逐条独立事务执行，单条失败（状态冲突/数据不合法）不影响整批；
     * 计费规则与单条派单一致，每条派单的 CAS 防重复收费语义完整保留。
     */
    public Map<String, Object> batchDispatch(BatchDispatchDTO dto, LoginUser operator) {
        DispatchDTO single = new DispatchDTO();
        single.setDriverId(dto.getDriverId());
        single.setTractorId(dto.getTractorId());

        int success = 0;
        List<String> failures = new ArrayList<>();
        for (Long id : dto.getIds()) {
            try {
                self.dispatch(id, single, operator);
                success++;
            } catch (BusinessException e) {
                EirOrder o = eirMapper.selectById(id);
                failures.add((o == null ? "单证#" + id : o.getEirNo()) + "：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failures.size());
        result.put("failures", failures);
        return result;
    }

    /**
     * 取消派单：DISPATCHED → CREATED。
     * 清除派单司机/拖车；若已生成收费记录则一并删除。
     * 已预约单证必须先取消预约才能取消派单。
     */
    @Transactional
    public void cancelDispatch(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        checkCompanyOwnership(eir, operator);

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.CANCEL_DISPATCH);

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setDriverId(null);
        update.setTractorId(null);
        EirStatus.casStatus(eirMapper, update, eir.getStatus());

        // 已缴费的收费记录不能随取消派单一并删除，避免钱已收但记录消失、账实不符
        FeeRecord fee = feeMapper.selectByEirId(id);
        if (fee != null && fee.getPayStatus() != null && fee.getPayStatus() == 1) {
            throw new BusinessException("该单证费用已缴纳，请先处理退费后再取消派单");
        }
        // 回滚派单时生成的未缴费收费记录
        feeMapper.deleteByEirId(id);
    }

    /* ==================== 内部方法 ==================== */

    private EirOrder mustExist(Long id) {
        EirOrder eir = eirMapper.selectById(id);
        if (eir == null) {
            throw new BusinessException("单证不存在");
        }
        return eir;
    }

    /**
     * COMPANY 角色只能操作本公司单证；ADMIN 不受限
     */
    private void checkCompanyOwnership(EirOrder eir, LoginUser operator) {
        if (Constants.ROLE_COMPANY.equals(operator.getRole())
                && !eir.getCompanyId().equals(operator.getCompanyId())) {
            throw new BusinessException("无权操作其他公司的单证");
        }
    }

    /**
     * 校验业务类型、箱尺寸、箱型合法性
     */
    private void validateBusiness(EirDTO dto) {
        List<String> types = Arrays.asList(
                Constants.BIZ_PI_F, Constants.BIZ_PI_E, Constants.BIZ_GR_E, Constants.BIZ_GR_F);
        if (!types.contains(dto.getBusinessType())) {
            throw new BusinessException("业务类型不合法");
        }
        if (dto.getContainerSize() != 20 && dto.getContainerSize() != 40) {
            throw new BusinessException("箱尺寸只能为 20 或 40");
        }
        if (!"GP".equals(dto.getContainerType()) && !"HQ".equals(dto.getContainerType())) {
            throw new BusinessException("箱型只能为 GP 或 HQ");
        }
        // 所属运输公司必须真实存在，防止产生孤儿单证（无 FK 约束，应用层兜底）
        if (companyMapper.selectById(dto.getCompanyId()) == null) {
            throw new BusinessException("所属运输公司不存在");
        }
    }

    /**
     * 生成单证编号：EIR + yyyyMMddHHmmss + 3位随机数；
     * 若前端传入编号则校验唯一后使用
     */
    private String generateEirNo(String input) {
        if (input != null && !input.isBlank()) {
            if (eirMapper.selectByEirNo(input.trim()) != null) {
                throw new BusinessException("单证编号已存在");
            }
            return input.trim();
        }
        String no;
        do {
            no = "EIR" + LocalDateTime.now().format(TIMESTAMP_FORMATTER)
                    + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        } while (eirMapper.selectByEirNo(no) != null);
        return no;
    }
}
