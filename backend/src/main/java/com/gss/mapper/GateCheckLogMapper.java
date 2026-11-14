package com.gss.mapper;

import com.gss.entity.GateCheckLog;
import com.gss.vo.GateCheckLogVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 闸口核验留痕 Mapper（M2）
 */
public interface GateCheckLogMapper {

    /** 新增一条核验记录 */
    int insert(GateCheckLog log);

    /** 分页查询核验记录（result 为空表示全部） */
    List<GateCheckLogVO> selectPage(@Param("result") String result,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("result") String result);
}
