package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.PageResult;
import com.gss.dto.CompanyDTO;
import com.gss.entity.TransportCompany;
import com.gss.mapper.TransportCompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 运输公司服务
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final TransportCompanyMapper companyMapper;

    /**
     * 分页查询公司
     */
    @Transactional(readOnly = true)
    public PageResult<TransportCompany> page(Integer page, Integer size, String name, String code) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<TransportCompany> rows = companyMapper.selectPage(name, code, (p - 1) * s, s);
        Long total = companyMapper.countPage(name, code);
        return PageResult.of(total, rows);
    }

    /**
     * 全部公司（下拉选择用）
     */
    @Transactional(readOnly = true)
    public List<TransportCompany> listAll() {
        return companyMapper.selectAll();
    }

    @Transactional(readOnly = true)
    public TransportCompany getById(Long id) {
        TransportCompany company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        return company;
    }

    public void create(CompanyDTO dto) {
        TransportCompany company = new TransportCompany();
        company.setCode(dto.getCode());
        company.setName(dto.getName());
        company.setContact(dto.getContact());
        company.setPhone(dto.getPhone());
        company.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        companyMapper.insert(company);
    }

    public void update(Long id, CompanyDTO dto) {
        TransportCompany company = getById(id);
        company.setCode(dto.getCode());
        company.setName(dto.getName());
        company.setContact(dto.getContact());
        company.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            company.setStatus(dto.getStatus());
        }
        companyMapper.update(company);
    }

    public void delete(Long id) {
        getById(id);
        if (companyMapper.hasRelatedRecords(id) == 1) {
            throw new BusinessException("该公司下仍有关联的司机/拖车/用户/单证/费用记录，无法删除");
        }
        companyMapper.deleteById(id);
    }
}
