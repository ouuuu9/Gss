package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.PageResult;
import com.gss.dto.DriverDTO;
import com.gss.entity.Driver;
import com.gss.mapper.DriverMapper;
import com.gss.vo.DriverVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 司机服务
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverMapper driverMapper;

    /**
     * 分页查询司机。
     * 数据权限：companyId 由 Controller 根据 JWT 身份传入（COMPANY 强制本公司）
     */
    public PageResult<DriverVO> page(Integer page, Integer size,
                                     String name, String phone, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<DriverVO> rows = driverMapper.selectPage(name, phone, companyId, (p - 1) * s, s);
        Long total = driverMapper.countPage(name, phone, companyId);
        return PageResult.of(total, rows);
    }

    /**
     * 全部司机（派单下拉用，由 Controller 限制公司范围）
     */
    public List<Driver> listAll() {
        return driverMapper.selectAll();
    }

    public Driver getById(Long id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }
        return driver;
    }

    public void create(DriverDTO dto) {
        Driver driver = new Driver();
        copy(driver, dto);
        driver.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        driverMapper.insert(driver);
    }

    public void update(Long id, DriverDTO dto) {
        Driver driver = getById(id);
        copy(driver, dto);
        if (dto.getStatus() != null) {
            driver.setStatus(dto.getStatus());
        }
        driverMapper.update(driver);
    }

    public void delete(Long id) {
        getById(id);
        driverMapper.deleteById(id);
    }

    private void copy(Driver driver, DriverDTO dto) {
        driver.setName(dto.getName());
        driver.setPhone(dto.getPhone());
        driver.setLicenseNo(dto.getLicenseNo());
        driver.setTractorId(dto.getTractorId());
        driver.setCompanyId(dto.getCompanyId());
    }
}
