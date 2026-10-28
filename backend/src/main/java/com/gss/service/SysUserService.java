package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.PageResult;
import com.gss.dto.UserDTO;
import com.gss.entity.Driver;
import com.gss.entity.SysUser;
import com.gss.entity.TransportCompany;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.SysUserMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统用户服务（ADMIN 管理）
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DriverMapper driverMapper;
    private final TransportCompanyMapper companyMapper;

    /**
     * 分页查询用户
     */
    @Transactional(readOnly = true)
    public PageResult<SysUserVO> page(Integer page, Integer size, String username, String role) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<SysUserVO> rows = sysUserMapper.selectPage(username, role, null, (p - 1) * s, s);
        Long total = sysUserMapper.countPage(username, role, null);
        return PageResult.of(total, rows);
    }

    /**
     * 用户详情
     */
    @Transactional(readOnly = true)
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 新增用户
     */
    public void create(UserDTO dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("初始密码不能为空");
        }
        if (sysUserMapper.selectByUsername(dto.getUsername().trim()) != null) {
            throw new BusinessException("用户名已存在");
        }
        validateRoleCompany(dto);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setRole(dto.getRole());
        user.setCompanyId(dto.getCompanyId());
        user.setDriverId(dto.getDriverId());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysUserMapper.insert(user);
    }

    /**
     * 修改用户（用户名不可改）
     */
    public void update(Long id, UserDTO dto) {
        SysUser user = mustExist(id);
        validateRoleCompany(dto);
        user.setName(dto.getName());
        user.setRole(dto.getRole());
        user.setCompanyId(dto.getCompanyId());
        user.setDriverId(dto.getDriverId());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        sysUserMapper.update(user);
    }

    /**
     * 删除用户（禁止删除当前登录账号、禁止删除最后一个管理员）
     */
    public void delete(Long id) {
        SysUser user = mustExist(id);
        LoginUser operator = UserContext.require();
        if (user.getId().equals(operator.getUserId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if ("ADMIN".equals(user.getRole()) && sysUserMapper.countPage(null, "ADMIN", null) <= 1) {
            throw new BusinessException("系统至少保留一个管理员账号");
        }
        sysUserMapper.deleteById(id);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        mustExist(id);
        sysUserMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    private SysUser mustExist(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 校验角色与公司/司机档案的对应关系：
     * - 码头端角色（ADMIN/GATE）不需要公司/司机
     * - 非码头端角色必须指定有效公司
     * - 司机账号必须关联本公司的司机档案，防止跨公司错配导致司机看不到自己的预约
     */
    private void validateRoleCompany(UserDTO dto) {
        String role = dto.getRole();
        boolean dockRole = "ADMIN".equals(role) || "GATE".equals(role);
        if (dockRole) {
            dto.setCompanyId(null);
            dto.setDriverId(null);
            return;
        }

        // 非码头端角色必须指定公司，且公司必须真实存在
        if (dto.getCompanyId() == null) {
            throw new BusinessException("请选择所属运输公司");
        }
        TransportCompany company = companyMapper.selectById(dto.getCompanyId());
        if (company == null) {
            throw new BusinessException("所属运输公司不存在");
        }

        // 司机账号必须关联司机档案，且档案必须属于当前公司
        if ("DRIVER".equals(role)) {
            if (dto.getDriverId() == null) {
                throw new BusinessException("司机账号必须关联司机档案");
            }
            Driver driver = driverMapper.selectById(dto.getDriverId());
            if (driver == null) {
                throw new BusinessException("关联的司机档案不存在");
            }
            if (!driver.getCompanyId().equals(dto.getCompanyId())) {
                throw new BusinessException("关联的司机档案不属于当前运输公司");
            }
        } else {
            dto.setDriverId(null);
        }
    }
}
