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
     * 可作废的状态集合：车辆未进闸前均可作废
     */
    private static final Set<String> CANCELLABLE_STATES = Set.of(
            Constants.ST_CREATED, Constants.ST_DISPATCHED, Constants.ST_APPOINTED);

    /**
     * 状态转移表：key=当前状态，value={event: 目标状态}
     */
    private final Map<String, Map<String, String>> transitions;

    public EirStateMachine() {
        Map<String, Map<String, String>> table = new HashMap<>();

        table.put(Constants.ST_CREATED, Map.of(
                EirEvent.DISPATCH, Constants.ST_DISPATCHED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        table.put(Constants.ST_DISPATCHED, Map.of(
                EirEvent.CANCEL_DISPATCH, Constants.ST_CREATED,
                EirEvent.APPOINT, Constants.ST_APPOINTED,
                EirEvent.CANCEL, Constants.ST_CANCELLED));

        table.put(Constants.ST_APPOINTED, Map.of(
                EirEvent.CANCEL_APPOINT, Constants.ST_DISPATCHED,
                EirEvent.EXPIRE_APPOINT, Constants.ST_DISPATCHED,
                EirEvent.GATE_IN, Constants.ST_GATE_IN,
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
