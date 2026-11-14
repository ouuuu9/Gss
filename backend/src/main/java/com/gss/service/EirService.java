package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.EirStatus;
import com.gss.common.PageResult;
import com.gss.dto.BatchDispatchDTO;
import com.gss.dto.DispatchDTO;
import com.gss.dto.EirApplyDTO;
import com.gss.dto.EirDTO;
import com.gss.dto.EirRejectDTO;
import com.gss.entity.Driver;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.Tractor;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final RefundService refundService;
    private final EirCanceller canceller;
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

        if (Constants.ROLE_COMPANY.equals(operator.getRole())
                && !eir.getCompanyId().equals(operator.getCompanyId())) {
            throw new BusinessException("无权查看其他公司的单证");
        }

        if (Constants.ROLE_DRIVER.equals(operator.getRole())) {
            if (operator.getDriverId() == null || !operator.getDriverId().equals(eir.getDriverId())) {
                throw new BusinessException("无权查看该单证");
            }
        }

        return eir;
    }

    /**
     * 录入单证（ADMIN 代录）：直接视为办单审核通过（M1）。
     * 码头端代录代表工作人员已核对单据，落库时同步写全审核留痕（review_by / review_time），
     * 保证不会出现「状态已通过但没有审核记录」的黑盒数据。
     */
    @Transactional
    public void create(EirDTO dto, LoginUser operator) {
        validateBusiness(dto);

        // 经状态机取目标状态：代录 = CREATED → APPROVED，禁止在 Service 里直接 setStatus 绕过规则
        String targetStatus = stateMachine.validateAndTransition(Constants.ST_CREATED, EirEvent.APPROVE);

        EirOrder eir = new EirOrder();
        eir.setEirNo(generateEirNo(dto.getEirNo()));
        eir.setBusinessType(dto.getBusinessType());
        eir.setContainerNo(dto.getContainerNo());
        eir.setContainerSize(dto.getContainerSize());
        eir.setContainerType(dto.getContainerType());
        eir.setCompanyId(dto.getCompanyId());
        eir.setStatus(targetStatus);
        eir.setRemark(dto.getRemark());
        // 代录即通过：审核留痕必须写全
        eir.setReviewStatus(Constants.REVIEW_APPROVED);
        eir.setReviewBy(operator.getUserId());
        eir.setReviewTime(LocalDateTime.now());
        eirMapper.insert(eir);
    }

    /**
     * 客户提交办单申请（COMPANY）：CREATED → SUBMITTED，等待码头管理员审核。
     * 作业公司强制取登录用户所属公司（前端传值不可信），避免替别家公司申请。
     */
    @Transactional
    public void apply(EirApplyDTO dto, LoginUser operator) {
        if (operator.getCompanyId() == null) {
            throw new BusinessException("当前账号未绑定运输公司，不能提交办单申请");
        }
        EirDTO business = new EirDTO();
        business.setBusinessType(dto.getBusinessType());
        business.setContainerNo(dto.getContainerNo());
        business.setContainerSize(dto.getContainerSize());
        business.setContainerType(dto.getContainerType());
        business.setCompanyId(operator.getCompanyId());
        validateBusiness(business);

        // 经状态机确认提交动作合法（CREATED → SUBMITTED）
        String targetStatus = stateMachine.validateAndTransition(Constants.ST_CREATED, EirEvent.SUBMIT);

        EirOrder eir = new EirOrder();
        eir.setEirNo(generateEirNo(null));
        eir.setBusinessType(dto.getBusinessType());
        eir.setContainerNo(dto.getContainerNo());
        eir.setContainerSize(dto.getContainerSize());
        eir.setContainerType(dto.getContainerType());
        eir.setCompanyId(operator.getCompanyId());
        eir.setStatus(targetStatus);
        eir.setRemark(dto.getRemark());
        eir.setApplyBy(operator.getUserId());
        eir.setApplyTime(LocalDateTime.now());
        eir.setReviewStatus(Constants.REVIEW_PENDING);
        eirMapper.insert(eir);
    }

    /**
     * 重新提交已驳回的办单申请（COMPANY）：REJECTED → SUBMITTED。
     * M1 不保留多轮申请历史：覆盖申请人与申请时间，清空上一轮审核信息与驳回原因；
     * 变更留痕走 @AuditLog 的操作日志。
     */
    @Transactional
    public void resubmit(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        checkCompanyOwnership(eir, operator);

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.SUBMIT);

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setApplyBy(operator.getUserId());
        update.setExpectedStatus(eir.getStatus());
        // updateSubmit 同时清空 review_by / review_time / reject_reason 并刷新 apply_time
        int rows = eirMapper.updateSubmit(update);
        if (rows == 0) {
            throw new BusinessException("单证状态已被其他操作变更，请刷新后重试");
        }
    }

    /**
     * 办单审核通过（ADMIN）：SUBMITTED → APPROVED。
     */
    @Transactional
    public void approve(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.APPROVE);

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(eir.getGateInTime());
        update.setGateOutTime(eir.getGateOutTime());
        update.setReviewStatus(Constants.REVIEW_APPROVED);
        update.setReviewBy(operator.getUserId());
        update.setReviewTime(LocalDateTime.now());
        // CAS：仅仍为 SUBMITTED 才生效，防止并发重复审核
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
    }

    /**
     * 办单审核驳回（ADMIN）：SUBMITTED → REJECTED。
     * 驳回必须在同一事务内取消该单证的有效预约并回补时段名额（R1），
     * 承载「驳回后预约自动失效、名额回补」的验收要求。
     */
    @Transactional
    public void reject(Long id, EirRejectDTO dto, LoginUser operator) {
        EirOrder eir = mustExist(id);

        String reason = dto.getRejectReason() == null ? "" : dto.getRejectReason().trim();
        if (reason.isEmpty()) {
            throw new BusinessException("驳回原因不能为空");
        }
        if (reason.length() > 200) {
            throw new BusinessException("驳回原因不能超过 200 字");
        }

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.REJECT);

        // 同事务：取消有效预约 + 回补 booked_count（条件 UPDATE，禁止先查后改）
        canceller.releaseActiveAppointment(eir);

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setGateInTime(eir.getGateInTime());
        update.setGateOutTime(eir.getGateOutTime());
        update.setReviewStatus(Constants.REVIEW_REJECTED);
        update.setReviewBy(operator.getUserId());
        update.setReviewTime(LocalDateTime.now());
        update.setRejectReason(reason);
        // CAS：仅仍为 SUBMITTED 才生效；失败则预约取消与名额回补一并回滚
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
    }

    /**
     * 撤销办单审核（ADMIN）：APPROVED / DISPATCHED / APPOINTED → REJECTED。
     *
     * 任务书要求「办单审核状态变更时对关联预约的同步约束」。驳回只能发生在 SUBMITTED，
     * 而 SUBMITTED 阶段还不存在预约；真正会产生级联的是「已通过/已派单/已预约后再撤销」，
     * 因此这里按状态分层处理：
     * - APPOINTED：先取消有效预约并回补时段名额（条件 UPDATE，命中一行才归还）；
     * - DISPATCHED：清空派单司机/拖车，预约前置随派单信息一并失效；
     * - APPROVED：无预约、无派单，直接置驳回。
     * 三个状态统一一次 CAS 落到 REJECTED，并写全审核留痕与撤销原因。
     * 已进闸及之后的状态不允许撤销（不在 M1 范围）。
     */
    @Transactional
    public void revokeReview(Long id, EirRejectDTO dto, LoginUser operator) {
        EirOrder eir = mustExist(id);

        String reason = dto.getRejectReason() == null ? "" : dto.getRejectReason().trim();
        if (reason.isEmpty()) {
            throw new BusinessException("撤销原因不能为空");
        }
        if (reason.length() > 200) {
            throw new BusinessException("撤销原因不能超过 200 字");
        }

        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.REVOKE_REVIEW);

        // 已预约的必须先取消预约并回补名额，再清派单信息；其余状态没有有效预约
        if (Constants.ST_APPOINTED.equals(eir.getStatus())) {
            canceller.releaseActiveAppointment(eir);
        }

        EirOrder update = new EirOrder();
        update.setId(id);
        update.setStatus(targetStatus);
        // 撤销审核后不再保留派单信息（updateStatus 会写入 NULL，并在 REJECTED 分支清空 dispatch_time）
        update.setTractorId(null);
        update.setDriverId(null);
        update.setGateInTime(eir.getGateInTime());
        update.setGateOutTime(eir.getGateOutTime());
        update.setReviewStatus(Constants.REVIEW_REJECTED);
        update.setReviewBy(operator.getUserId());
        update.setReviewTime(LocalDateTime.now());
        update.setRejectReason(reason);
        // CAS：仅当前状态与读取时一致才落库；失败则预约取消与名额回补一并回滚
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
    }

    /**
     * 办单审核待办分页（ADMIN）：只返回 SUBMITTED 的单证，按申请时间先来先审。
     */
    @Transactional(readOnly = true)
    public PageResult<EirOrderVO> reviewPage(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<EirOrderVO> rows = eirMapper.selectReviewPage((p - 1) * s, s);
        return PageResult.of(eirMapper.countReviewPending(), rows);
    }

    /**
     * 修改单证：仅允许在 CREATED / DISPATCHED 状态修改基础信息，
     * 已进入闸口作业的单证禁止修改
     */
    @Transactional
    public void update(Long id, EirDTO dto) {
        EirOrder eir = mustExist(id);
        // 只有尚未进入闸口作业的单证才允许修改基础信息
        boolean editable = Constants.ST_CREATED.equals(eir.getStatus())
                || Constants.ST_APPROVED.equals(eir.getStatus())
                || Constants.ST_REJECTED.equals(eir.getStatus())
                || Constants.ST_DISPATCHED.equals(eir.getStatus());
        if (!editable) {
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
     * 未缴费：取消有效预约并直接作废；
     * 已缴费：不直接作废，改为生成待审批的退款申请，审批通过后才退款并作废。
     * 已作废单证不可重复作废。
     *
     * @return CANCELLED 已直接作废；REFUND_PENDING 已提交退款申请待审批
     */
    @Transactional
    public String cancel(Long id, LoginUser operator) {
        EirOrder eir = mustExist(id);
        checkCompanyOwnership(eir, operator);

        if (Constants.ST_CANCELLED.equals(eir.getStatus())) {
            throw new BusinessException("单证已作废，请勿重复操作");
        }
        if (!stateMachine.canCancel(eir.getStatus())) {
            throw new BusinessException("车辆已进闸作业，单证不能作废");
        }

        FeeRecord fee = feeMapper.selectByEirId(id);
        if (fee != null && fee.getPayStatus() != null && fee.getPayStatus() == Constants.PAY_REFUNDED) {
            throw new BusinessException("该单证费用已退款，请勿重复操作");
        }
        if (fee != null && fee.getPayStatus() != null && fee.getPayStatus() == Constants.PAY_PAID) {
            // 已缴费：先进入退款审批，审批通过前保持原状态
            refundService.apply(eir, fee, operator);
            return "REFUND_PENDING";
        }

        canceller.cancelNow(eir);
        return "CANCELLED";
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
        ensureReviewPassedForDispatch(eir);

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
                // 失败项带上单证编号，前端可直接定位；查不到时退回用 ID 标识
                EirOrder failedEir = eirMapper.selectById(id);
                String eirLabel;
                if (failedEir == null) {
                    eirLabel = "单证#" + id;
                } else {
                    eirLabel = failedEir.getEirNo();
                }
                failures.add(eirLabel + "：" + e.getMessage());
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

    private EirOrder mustExist(Long id) {
        EirOrder eir = eirMapper.selectById(id);
        if (eir == null) {
            throw new BusinessException("单证不存在");
        }
        return eir;
    }

    /**
     * 派单前置（M1/R2）：办单申请必须审核通过。
     * 未审核与已驳回分别给出可操作提示（驳回时带上原因），避免客户无从下手。
     */
    private void ensureReviewPassedForDispatch(EirOrder eir) {
        if (Constants.ST_SUBMITTED.equals(eir.getStatus())) {
            throw new BusinessException("单证办单申请尚未审核通过，暂不能派单");
        }
        if (Constants.ST_REJECTED.equals(eir.getStatus())) {
            String reason = eir.getRejectReason() == null || eir.getRejectReason().isBlank()
                    ? "未填写原因" : eir.getRejectReason();
            throw new BusinessException("单证办单申请已被驳回（原因：" + reason + "），请修改后重新提交，暂不能派单");
        }
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
        String businessType = dto.getBusinessType();
        boolean knownBusinessType = Constants.BIZ_PI_F.equals(businessType)
                || Constants.BIZ_PI_E.equals(businessType)
                || Constants.BIZ_GR_E.equals(businessType)
                || Constants.BIZ_GR_F.equals(businessType);
        if (!knownBusinessType) {
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
