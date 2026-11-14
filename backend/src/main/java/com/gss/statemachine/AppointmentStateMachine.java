package com.gss.statemachine;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 预约状态机（M2）。
 *
 * ACTIVE --VERIFY--> VERIFIED --RELEASE--> RELEASED
 * ACTIVE --CANCEL--> CANCELLED
 * ACTIVE --EXPIRE--> NO_SHOW（失约）
 *
 * 与 EirStateMachine 同一写法：转移表集中管理，Service 通过 validateAndTransition
 * 校验事件并拿到目标状态，禁止在 Service 里直接 setStatus。
 */
@Component
public class AppointmentStateMachine {

    private final Map<String, Map<String, String>> transitions;

    public AppointmentStateMachine() {
        Map<String, Map<String, String>> table = new HashMap<>();
        table.put(Constants.APPT_ACTIVE, Map.of(
                AppointmentEvent.VERIFY, Constants.APPT_VERIFIED,
                AppointmentEvent.CANCEL, Constants.APPT_CANCELLED,
                AppointmentEvent.EXPIRE, Constants.APPT_NO_SHOW));
        table.put(Constants.APPT_VERIFIED, Map.of(
                AppointmentEvent.RELEASE, Constants.APPT_RELEASED));
        table.put(Constants.APPT_RELEASED, Collections.emptyMap());
        table.put(Constants.APPT_CANCELLED, Collections.emptyMap());
        table.put(Constants.APPT_NO_SHOW, Collections.emptyMap());
        // 历史 EXPIRED 视为终态，不再流转
        table.put(Constants.APPT_EXPIRED, Collections.emptyMap());
        this.transitions = Collections.unmodifiableMap(table);
    }

    /**
     * 校验预约状态转移是否合法，不合法抛 BusinessException
     */
    public String validateAndTransition(String currentStatus, String event) {
        Map<String, String> eventMap = transitions.get(currentStatus);
        String targetStatus = eventMap == null ? null : eventMap.get(event);
        if (targetStatus == null) {
            throw new BusinessException("预约当前状态【" + currentStatus + "】不允许执行操作：" + event);
        }
        return targetStatus;
    }

    /** 该预约是否已核验（核验后不再判失约） */
    public boolean hasVerified(String status) {
        return Constants.APPT_VERIFIED.equals(status) || Constants.APPT_RELEASED.equals(status);
    }
}
