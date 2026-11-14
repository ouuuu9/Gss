package com.gss.service;

import com.gss.common.Constants;
import com.gss.common.EirStatus;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.EirOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 单证作废的公共落库动作：取消有效预约、归还时段名额、CAS 置为已作废。
 * 未缴费作废（EirService）与审批通过后的退款作废（RefundService）共用，
 * 保证两条路径的作废语义一致。
 */
@Component
@RequiredArgsConstructor
public class EirCanceller {

    private final EirOrderMapper eirMapper;
    private final AppointmentMapper appointmentMapper;
    private final ApptSlotCapacityMapper slotCapacityMapper;

    /**
     * 立即作废单证。必须在外层事务内调用：预约取消、名额归还与状态 CAS 同生共死。
     *
     * @param eir 已校验可作废的单证快照
     */
    public void cancelNow(EirOrder eir) {
        // 取消有效预约并归还名额
        releaseActiveAppointment(eir);

        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(Constants.ST_CANCELLED);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        // CAS：仅当前状态与读取时一致才落库，与派单/进闸并发时作废失败并回滚
        EirStatus.casStatus(eirMapper, update, eir.getStatus());
    }

    /**
     * 只取消单证当前的有效预约并归还时段名额，不改变单证本身状态。
     * 办单审核驳回（M1）与作废（cancelNow）共用，保证两条路径的名额回补语义一致。
     * 必须在外层事务内调用。
     */
    public void releaseActiveAppointment(EirOrder eir) {
        // 取消有效预约：CAS 置为已取消成功后归还时段名额（防止与预约取消并发导致名额重复释放）
        Appointment activeAppt = appointmentMapper.selectActiveByEirId(eir.getId());
        if (activeAppt == null) {
            return;
        }
        int rows = appointmentMapper.updateStatusIf(activeAppt.getId(),
                Constants.APPT_CANCELLED, Constants.APPT_ACTIVE);
        if (rows == 1 && StringUtils.hasText(activeAppt.getTimeSlot())) {
            slotCapacityMapper.release(activeAppt.getApptDate(), activeAppt.getTimeSlot());
        }
    }
}
