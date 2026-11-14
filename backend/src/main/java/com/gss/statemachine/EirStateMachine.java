package com.gss.statemachine;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * EIR 单证状态机引擎。
 * 以状态转移表的形式集中管理单证状态流转规则，Service 层通过 {@link #validateAndTransition}
 * 校验事件合法性并获取目标状态，避免在各业务方法中硬编码状态判断。
 */
@Component
public class EirStateMachine {

    /**
     * 可作废的状态集合：车辆未进闸前均可作废（M1 起含办单申请/审核各态）
     */
    private static final Set<String> CANCELLABLE_STATES = Set.of(
            Constants.ST_CREATED, Constants.ST_SUBMITTED, Constants.ST_APPROVED, Constants.ST_REJECTED,
            Constants.ST_DISPATCHED, Constants.ST_APPOINTED);

    /**
     * 状态转移表：key=当前状态，value={event: 目标状态}
     *
     * M1 办单审核闭环（R1/R2）：
     * CREATED --SUBMIT--> SUBMITTED --APPROVE--> APPROVED --DISPATCH--> DISPATCHED --APPOINT--> APPOINTED
     *                         |--REJECT--> REJECTED --SUBMIT--> SUBMITTED（重新提交）
     * 撤销审核（REVOKE_REVIEW）：APPROVED / DISPATCHED / APPOINTED → REJECTED
     * 取消派单回到 APPROVED（不再回到 CREATED，避免绕过审核）
     */
    private final Map<String, Map<String, String>> transitions;

    public EirStateMachine() {
        Map<String, Map<String, String>> table = new HashMap<>();

        // CREATED 仅作历史初始态兼容：客户可提交（→SUBMITTED）；
        // 管理员代录（POST /api/eir）录入即视为审核通过（→APPROVED），两条路径并存
        table.put(Constants.ST_CREATED, Map.of(
                EirEvent.SUBMIT, Constants.ST_SUBMITTED,
                EirEvent.APPROVE, Constants.ST_APPROVED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        // 待审核：可审核通过、驳回、作废
        table.put(Constants.ST_SUBMITTED, Map.of(
                EirEvent.APPROVE, Constants.ST_APPROVED,
                EirEvent.REJECT, Constants.ST_REJECTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        // 审核通过：可派单/作废，或撤销审核（→REJECTED）
        table.put(Constants.ST_APPROVED, Map.of(
                EirEvent.DISPATCH, Constants.ST_DISPATCHED,
                EirEvent.REVOKE_REVIEW, Constants.ST_REJECTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        // 已驳回：可重新提交或作废
        table.put(Constants.ST_REJECTED, Map.of(
                EirEvent.SUBMIT, Constants.ST_SUBMITTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        // 已派单：取消派单回到 APPROVED（保持已审核状态）；撤销审核 → REJECTED（同时清派单信息）
        table.put(Constants.ST_DISPATCHED, Map.of(
                EirEvent.CANCEL_DISPATCH, Constants.ST_APPROVED,
                EirEvent.APPOINT, Constants.ST_APPOINTED,
                EirEvent.REVOKE_REVIEW, Constants.ST_REJECTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        // 已预约：撤销审核 → REJECTED（先取消预约+回补名额，再清派单信息）
        table.put(Constants.ST_APPOINTED, Map.of(
                EirEvent.CANCEL_APPOINT, Constants.ST_DISPATCHED,
                EirEvent.EXPIRE_APPOINT, Constants.ST_DISPATCHED,
                EirEvent.GATE_IN, Constants.ST_GATE_IN,
                EirEvent.REVOKE_REVIEW, Constants.ST_REJECTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        table.put(Constants.ST_GATE_IN, Map.of(
                EirEvent.GATE_OUT, Constants.ST_GATE_OUT));

        table.put(Constants.ST_GATE_OUT, Map.of(
                EirEvent.AUDIT, Constants.ST_DONE));

        table.put(Constants.ST_DONE, Collections.emptyMap());
        table.put(Constants.ST_CANCELLED, Collections.emptyMap());

        this.transitions = Collections.unmodifiableMap(table);
    }

    /**
     * 校验状态转移是否合法，不合法抛 BusinessException
     * @param currentStatus 当前状态
     * @param event 触发事件
     * @return 目标状态
     */
    public String validateAndTransition(String currentStatus, String event) {
        Map<String, String> eventMap = transitions.get(currentStatus);
        if (eventMap == null) {
            throw new BusinessException("当前状态【" + currentStatus + "】不允许执行操作：" + event);
        }
        String targetStatus = eventMap.get(event);
        if (targetStatus == null) {
            throw new BusinessException("当前状态【" + currentStatus + "】不允许执行操作：" + event);
        }
        return targetStatus;
    }

    /**
     * 判断状态是否可作废（CREATED/DISPATCHED/APPOINTED 可作废，已进闸不可作废）
     */
    public boolean canCancel(String currentStatus) {
        return CANCELLABLE_STATES.contains(currentStatus);
    }
}
