package com.gss.mapper;

import com.gss.entity.Tractor;
import com.gss.vo.TractorVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拖车 Mapper
 */
public interface TractorMapper {

    /** 按IC卡号查询（刷卡进/出闸用） */
    Tractor selectByIcCardNo(@Param("icCardNo") String icCardNo);

    /** 按ID查询 */
    Tractor selectById(@Param("id") Long id);

    /** 分页查询（含公司名称） */
    List<TractorVO> selectPage(@Param("plateNo") String plateNo,
                               @Param("icCardNo") String icCardNo,
                               @Param("companyId") Long companyId,
                               @Param("offset") int offset,
                               @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("plateNo") String plateNo,
                   @Param("icCardNo") String icCardNo,
                   @Param("companyId") Long companyId);

    /** 新增 */
    int insert(Tractor tractor);

    /** 修改 */
    int update(Tractor tractor);

    /** 删除 */
    int deleteById(@Param("id") Long id);
}
