package com.gss.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 预约时段容量 Mapper。
 * 容量扣减/释放全部使用单条 UPDATE 的原子语义，天然防并发超约。
 */
public interface ApptSlotCapacityMapper {

    /**
     * 幂等初始化某日期某时段的容量记录（已存在则跳过）
     */
    int insertIgnore(@Param("apptDate") LocalDate apptDate,
                     @Param("timeSlot") String timeSlot,
                     @Param("capacity") int capacity);

    /**
     * 原子占用一个时段名额：剩余名额足够才扣减，影响行数为 0 即已约满
     */
    int tryBook(@Param("apptDate") LocalDate apptDate, @Param("timeSlot") String timeSlot);

    /**
     * 释放一个时段名额（取消预约/超时释放时归还）
     */
    int release(@Param("apptDate") LocalDate apptDate, @Param("timeSlot") String timeSlot);

    /**
     * 查询某日期各时段剩余容量（未初始化的时段按默认容量返回）
     * @param slots 全部时段（Constants.TIME_SLOTS）
     * @param defaultCapacity 未初始化时段的默认容量
     */
    List<Map<String, Object>> selectRemain(@Param("date") String date,
                                           @Param("slots") List<String> slots,
                                           @Param("defaultCapacity") int defaultCapacity);
}
