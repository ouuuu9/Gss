package com.gss.common;

/**
 * 系统常量：单证状态、业务类型、角色
 */
public final class Constants {

    private Constants() {
    }

    /* ==================== 单证状态机 ==================== */
    /** 已录入 */
    public static final String ST_CREATED = "CREATED";
    /** 已派单 */
    public static final String ST_DISPATCHED = "DISPATCHED";
    /** 已预约 */
    public static final String ST_APPOINTED = "APPOINTED";
    /** 已进闸 */
    public static final String ST_GATE_IN = "GATE_IN";
    /** 已出闸 */
    public static final String ST_GATE_OUT = "GATE_OUT";
    /** 已完成 */
    public static final String ST_DONE = "DONE";
    /** 已作废 */
    public static final String ST_CANCELLED = "CANCELLED";

    /* ==================== 办单申请/审核（M1） ==================== */
    /** 已提交办单申请（等待管理员审核） */
    public static final String ST_SUBMITTED = "SUBMITTED";
    /** 办单审核通过（可派单） */
    public static final String ST_APPROVED = "APPROVED";
    /** 办单申请已驳回（可修改后重新提交） */
    public static final String ST_REJECTED = "REJECTED";

    /* ============ 办单审核状态（review 与闸口审单 audit 语义分开，禁止混用） ============ */
    /** 待审核 */
    public static final String REVIEW_PENDING = "PENDING";
    /** 审核通过 */
    public static final String REVIEW_APPROVED = "APPROVED";
    /** 审核驳回 */
    public static final String REVIEW_REJECTED = "REJECTED";

    /* ==================== 预约状态 ==================== */
    public static final String APPT_ACTIVE = "ACTIVE";
    public static final String APPT_CANCELLED = "CANCELLED";
    /** 已核验（闸口凭证核验通过，尚未出闸） */
    public static final String APPT_VERIFIED = "VERIFIED";
    /** 已放行（出闸后释放） */
    public static final String APPT_RELEASED = "RELEASED";
    /** 失约（时段结束仍未核验，定时任务置位；M2 起取代旧的 EXPIRED） */
    public static final String APPT_NO_SHOW = "NO_SHOW";
    /** 旧状态，仅用于历史数据识别（迁移时统一改为 NO_SHOW） */
    public static final String APPT_EXPIRED = "EXPIRED";

    /* ==================== 闸口核验 ==================== */
    /** 核验窗口：时段开始前/结束后各允许 30 分钟 */
    public static final int VERIFY_WINDOW_MINUTES = 30;
    /** 核验结果 */
    public static final String CHECK_PASS = "PASS";
    public static final String CHECK_FAIL = "FAIL";
    /** 核验异常类型 */
    public static final String ABNORMAL_NO_APPOINTMENT = "NO_APPOINTMENT";
    public static final String ABNORMAL_CANCELLED = "CANCELLED";
    public static final String ABNORMAL_EXPIRED = "EXPIRED";
    public static final String ABNORMAL_DOC_MISMATCH = "DOC_MISMATCH";
    public static final String ABNORMAL_EARLY = "EARLY";
    public static final String ABNORMAL_LATE = "LATE";

    /* ==================== 预约时段 ==================== */
    /** 8 个两小时进港时段（06:00-22:00） */
    public static final String[] TIME_SLOTS = {
            "06:00-08:00", "08:00-10:00", "10:00-12:00", "12:00-14:00",
            "14:00-16:00", "16:00-18:00", "18:00-20:00", "20:00-22:00"
    };
    /** 单时段默认最大预约数 */
    public static final int DEFAULT_SLOT_CAPACITY = 10;

    /* ==================== 业务类型 ==================== */
    /** 提重 */
    public static final String BIZ_PI_F = "PI-F";
    /** 提空 */
    public static final String BIZ_PI_E = "PI-E";
    /** 还空 */
    public static final String BIZ_GR_E = "GR-E";
    /** 还重（需要缴费） */
    public static final String BIZ_GR_F = "GR-F";

    /* ==================== 缴费状态 ==================== */
    /** 未缴 */
    public static final int PAY_UNPAID = 0;
    /** 已缴 */
    public static final int PAY_PAID = 1;
    /** 已退 */
    public static final int PAY_REFUNDED = 2;

    /* ==================== 退款状态 ==================== */
    /** 待审批 */
    public static final String REFUND_PENDING = "PENDING";
    /** 已退款 */
    public static final String REFUND_APPROVED = "APPROVED";
    /** 已驳回 */
    public static final String REFUND_REJECTED = "REJECTED";

    /* ==================== 角色 ==================== */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_GATE = "GATE";
    public static final String ROLE_COMPANY = "COMPANY";
    public static final String ROLE_DRIVER = "DRIVER";
}
