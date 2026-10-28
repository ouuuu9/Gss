package com.gss.mapper;

import com.gss.entity.Driver;
import com.gss.vo.DriverVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 司机 Mapper
 */
public interface DriverMapper {

    /** 查询全部司机 */
    List<Driver> selectAll();

    /** 按ID查询 */
    Driver selectById(@Param("id") Long id);

    /** 分页查询（含公司名称、绑定车牌） */
    List<DriverVO> selectPage(@Param("name") String name,
                              @Param("phone") String phone,
                              @Param("companyId") Long companyId,
                              @Param("offset") int offset,
                              @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("name") String name,
                   @Param("phone") String phone,
                   @Param("companyId") Long companyId);

    /** 新增 */
    int insert(Driver driver);

    /** 修改 */
    int update(Driver driver);

    /** 删除 */
    int deleteById(@Param("id") Long id);

    /** 检查该司机是否仍有未完成的单证引用（已派单/已预约/已进闸/已出闸状态） */
    int hasActiveEir(@Param("driverId") Long driverId);

    /** 检查该司机是否存在历史单证（已完成/已作废），存在则删除时应改为停用以保留追溯 */
    int hasEirHistory(@Param("driverId") Long driverId);
}
