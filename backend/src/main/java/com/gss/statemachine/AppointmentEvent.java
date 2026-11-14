package com.gss.statemachine;

/**
 * 预约状态事件（M2）。
 * 与 EirEvent 一样集中登记，Service 只允许通过状态机取目标状态。
 */
public final class AppointmentEvent {

    private AppointmentEvent() {
    }

    /** 闸口凭证核验通过 */
    public static final String VERIFY = "VERIFY";
    /** 出闸放行 */
    public static final String RELEASE = "RELEASE";
    /** 取消预约 */
    public static final String CANCEL = "CANCEL";
    /** 超时未核验（失约） */
    public static final String EXPIRE = "EXPIRE";
}
