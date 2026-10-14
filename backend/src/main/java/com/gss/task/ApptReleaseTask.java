package com.gss.task;

import com.gss.common.Constants;
import com.gss.entity.Appointment;
import com.gss.entity.EirOrder;
import com.gss.mapper.ApptSlotCapacityMapper;
import com.gss.mapper.AppointmentMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.statemachine.EirEvent;
import com.gss.statemachine.EirStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 预约超时自动释放定时任务：
 * 每 60 秒扫描一次，将已过时段仍未进闸的有效预约置为 EXPIRED、
 * 归还时段容量，并将关联单证从已预约回退到已派单，释放资源供重新预约。
 * 单条处理异常不阻塞整批；CAS 门槛保证与取消/进闸操作并发时不会重复释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApptReleaseTask {

    private final AppointmentMapper appointmentMapper;
    private final ApptSlotCapacityMapper slotCapacityMapper;
    private final EirOrderMapper eirMapper;
    private final EirStateMachine stateMachine;

    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredAppointments() {
        List<Appointment> expiredList;
        try {
            expiredList = appointmentMapper.selectExpired();
        } catch (Exception e) {
            log.error("扫描过期预约失败", e);
            return;
        }
        for (Appointment appt : expiredList) {
            try {
                releaseOne(appt);
            } catch (Exception e) {
                log.error("释放过期预约失败, appointmentId={}", appt.getId(), e);
            }
        }
    }

    private void releaseOne(Appointment appt) {
        // CAS：仅仍为 ACTIVE 才置为已过期，防止与取消/进闸并发时重复处理
        int rows = appointmentMapper.updateStatusIf(appt.getId(), Constants.APPT_EXPIRED, Constants.APPT_ACTIVE);
        if (rows == 0) {
            return;
        }
        if (StringUtils.hasText(appt.getTimeSlot())) {
            slotCapacityMapper.release(appt.getApptDate(), appt.getTimeSlot());
        }
        EirOrder eir = eirMapper.selectById(appt.getEirId());
        if (eir == null || !Constants.ST_APPOINTED.equals(eir.getStatus())) {
            return;
        }
        String targetStatus = stateMachine.validateAndTransition(eir.getStatus(), EirEvent.EXPIRE_APPOINT);
        EirOrder update = new EirOrder();
        update.setId(eir.getId());
        update.setStatus(targetStatus);
        update.setTractorId(eir.getTractorId());
        update.setDriverId(eir.getDriverId());
        update.setExpectedStatus(eir.getStatus());
        eirMapper.updateStatus(update);
    }
}
