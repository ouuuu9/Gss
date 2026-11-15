package com.gss.config;

import com.gss.mapper.GateLaneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动数据自检（M5）。
 *
 * 项目全库不放外键（一致性由应用层保证，见设计说明），因此这里做一次轻量自检：
 * appointment.lane_id 是否指向存在的车道。发现孤儿行只 WARN、不阻断启动，
 * 目的是让测试库/演示库造数产生脏数据时第一时间暴露。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataChecker implements CommandLineRunner {

    private final GateLaneMapper laneMapper;

    @Override
    public void run(String... args) {
        try {
            Long orphans = laneMapper.countOrphanLaneAppointments();
            if (orphans != null && orphans > 0) {
                List<Long> sample = laneMapper.selectOrphanLaneAppointmentIds(5);
                log.warn("[数据自检] appointment.lane_id 有 {} 条找不到对应车道，样例 id={}；"
                        + "按车道分组会出现空分组，请修数据（不阻断启动）", orphans, sample);
            } else {
                log.info("[数据自检] appointment.lane_id 完整，无孤儿记录");
            }
        } catch (Exception e) {
            log.warn("[数据自检] 执行失败（忽略）：{}", e.getMessage());
        }
    }
}
