package com.gss.mapper;

import com.gss.entity.SysUser;
import com.gss.vo.SysUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper
 */
public interface SysUserMapper {

    /** 用户总数 */
    Long countAll();

    /** 按用户名查询（登录用） */
    SysUser selectByUsername(@Param("username") String username);

    /** 按ID查询 */
    SysUser selectById(@Param("id") Long id);

    /** 分页查询用户（含公司名称） */
    List<SysUserVO> selectPage(@Param("username") String username,
                               @Param("role") String role,
                               @Param("companyId") Long companyId,
                               @Param("offset") int offset,
                               @Param("size") int size);

    /** 分页统计 */
    Long countPage(@Param("username") String username,
                   @Param("role") String role,
                   @Param("companyId") Long companyId);

    /** 新增用户 */
    int insert(SysUser user);

    /** 修改用户 */
    int update(SysUser user);

    /** 删除用户 */
    int deleteById(@Param("id") Long id);

    /** 重置密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
