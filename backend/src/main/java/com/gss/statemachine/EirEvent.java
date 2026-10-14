package com.gss.statemachine;

public final class EirEvent {
    private EirEvent() {}
    public static final String DISPATCH = "DISPATCH";           // 派单
    public static final String CANCEL_DISPATCH = "CANCEL_DISPATCH"; // 取消派单
    public static final String APPOINT = "APPOINT";             // 预约
    public static final String CANCEL_APPOINT = "CANCEL_APPOINT"; // 取消预约
    public static final String EXPIRE_APPOINT = "EXPIRE_APPOINT"; // 预约超时自动释放
    public static final String GATE_IN = "GATE_IN";            // 进闸
    public static final String GATE_OUT = "GATE_OUT";          // 出闸
    public static final String AUDIT = "AUDIT";                 // 审单
    public static final String CANCEL = "CANCEL";               // 作废
}
