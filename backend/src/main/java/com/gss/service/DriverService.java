package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.PageResult;
import com.gss.dto.DriverDTO;
import com.gss.entity.Driver;
import com.gss.entity.Tractor;
import com.gss.entity.TransportCompany;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.vo.DriverVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 司机服务
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverMapper driverMapper;
    private final TractorMapper tractorMapper;
    private final TransportCompanyMapper companyMapper;

    /**
     * 分页查询司机。
     * 数据权限：companyId 由 Controller 根据 JWT 身份传入（COMPANY 强制本公司）
     */
    @Transactional(readOnly = true)
    public PageResult<DriverVO> page(Integer page, Integer size,
                                     String name, String phone, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<DriverVO> rows = driverMapper.selectPage(name, phone, companyId, (p - 1) * s, s);
        Long total = driverMapper.countPage(name, phone, companyId);
        return PageResult.of(total, rows);
    }

    /**
     * 全部司机（派单下拉用；COMPANY 只返回本公司司机，已停用的不可再被选择）
     */
    @Transactional(readOnly = true)
    public List<DriverVO> listAll() {
        LoginUser user = UserContext.require();
        Long companyId = Constants.ROLE_COMPANY.equals(user.getRole()) ? user.getCompanyId() : null;
        return driverMapper.selectPage(null, null, companyId, 0, 1000).stream()
                .filter(d -> Integer.valueOf(1).equals(d.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Driver getById(Long id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }
        checkOwnership(driver);
        return driver;
    }

    public void create(DriverDTO dto) {
        validateCompany(dto.getCompanyId());
        validateTractor(dto.getTractorId(), dto.getCompanyId());
        Driver driver = new Driver();
        copy(driver, dto);
        driver.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        driverMapper.insert(driver);
    }

    public void update(Long id, DriverDTO dto) {
        Driver driver = getById(id);
        validateCompany(dto.getCompanyId());
        validateTractor(dto.getTractorId(), dto.getCompanyId());
        copy(driver, dto);
        if (dto.getStatus() != null) {
            driver.setStatus(dto.getStatus());
        }
        driverMapper.update(driver);
    }

    /**
     * 删除司机。
     * 有进行中单证 → 拒绝；仅有历史单证（已完成/已作废）→ 软删除（置为停用），
     * 保留历史单证的司机姓名展示；无任何单证引用 → 物理删除。
     *
     * @return true=物理删除，false=软删除（已停用）
     */
    public boolean delete(Long id) {
        getById(id);
        if (driverMapper.hasActiveEir(id) == 1) {
            throw new BusinessException("该司机仍有关联的进行中单证（已派单/已预约/已进闸），无法删除");
        }
        if (driverMapper.hasEirHistory(id) == 1) {
            Driver driver = new Driver();
            driver.setId(id);
            driver.setStatus(0);
            driverMapper.update(driver);
            return false;
        }
        driverMapper.deleteById(id);
        return true;
    }

    /**
     * 数据权限：COMPANY 只能查看/修改/删除本公司司机，防止按 ID 越权操作他公司档案
     */
    private void checkOwnership(Driver driver) {
        LoginUser user = UserContext.require();
        if (Constants.ROLE_COMPANY.equals(user.getRole())
                && !driver.getCompanyId().equals(user.getCompanyId())) {
            throw new BusinessException("无权操作其他公司的司机");
        }
    }

    private void copy(Driver driver, DriverDTO dto) {
        driver.setName(dto.getName());
        driver.setPhone(dto.getPhone());
        driver.setLicenseNo(dto.getLicenseNo());
        driver.setTractorId(dto.getTractorId());
        driver.setCompanyId(dto.getCompanyId());
    }

    /**
     * 校验所属运输公司真实存在，防止产生孤儿司机记录（无 FK 约束，应用层兜底）
     */
    private void validateCompany(Long companyId) {
        TransportCompany company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("所属运输公司不存在");
        }
    }

    /**
     * 校验绑定拖车的归属：tractorId 非空时拖车必须存在且属于指定公司，
     * 防止 COMPANY A 创建司机时跨公司绑定 COMPANY B 的拖车
     */
    private void validateTractor(Long tractorId, Long companyId) {
        if (tractorId == null) {
            return;
        }
        Tractor tractor = tractorMapper.selectById(tractorId);
        if (tractor == null) {
            throw new BusinessException("绑定的拖车不存在");
        }
        if (!tractor.getCompanyId().equals(companyId)) {
            throw new BusinessException("绑定的拖车不属于当前运输公司");
        }
    }
}
