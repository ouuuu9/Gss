package com.gss.common;

import com.gss.entity.EirOrder;
import com.gss.mapper.EirOrderMapper;

/**
 * 单证状态条件更新（CAS）公共方法。
 * 三个 Service 类（EirService / GateService / AppointmentService）原来各自 copy 一份，
 * 逻辑完全相同，统一收敛在此处，避免规则改一处漏一处。
 */
public final class EirStatus {

    private EirStatus() {
    }

    /**
     * 条件更新单证状态。
     * 仅当数据库当前 status 等于 expectedStatus 时落库；
     * 影响行数为 0 说明被并发操作抢先变更，抛出冲突异常由事务回滚。
     */
    public static void casStatus(EirOrderMapper mapper, EirOrder update, String expectedStatus) {
        update.setExpectedStatus(expectedStatus);
        int rows = mapper.updateStatus(update);
        if (rows == 0) {
            throw new BusinessException("单证状态已被其他操作变更，请刷新后重试");
        }
    }
}
