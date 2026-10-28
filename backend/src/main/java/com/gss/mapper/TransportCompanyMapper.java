package com.gss.mapper;

import com.gss.entity.TransportCompany;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运输公司 Mapper
 */
public interface TransportCompanyMapper {

    /** 查询全部公司（下拉选择用） */
    List<TransportCompany> selectAll();

    /** 按ID查询 */
    TransportCompany selectById(@Param("id") Long id);

    /** 分页查询 */
    List<TransportCompany> selectPage(@Param("name") String name,
                                      @Param("code") String code,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("name") String name, @Param("code") String code);

    /** 新增 */
    int insert(TransportCompany company);

    /** 修改 */
    int update(TransportCompany company);

    /** 删除 */
    int deleteById(@Param("id") Long id);

    /** 检查该公司是否仍有司机/拖车/用户/单证/费用引用 */
    int hasRelatedRecords(@Param("companyId") Long companyId);
}
