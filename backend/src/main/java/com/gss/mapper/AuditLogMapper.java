package com.gss.mapper;

import com.gss.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 操作审计日志 Mapper
 */
public interface AuditLogMapper {

    /** 写入审计日志 */
    int insert(AuditLogEntity log);

    /** 分页查询（username/operation 模糊匹配，success 精确匹配，startTime/endTime 时间范围） */
    List<AuditLogEntity> selectPage(@Param("username") String username,
                                    @Param("operation") String operation,
                                    @Param("success") Integer success,
                                    @Param("startTime") LocalDate startTime,
                                    @Param("endTime") LocalDate endTime,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("username") String username,
                   @Param("operation") String operation,
                   @Param("success") Integer success,
                   @Param("startTime") LocalDate startTime,
                   @Param("endTime") LocalDate endTime);
}
