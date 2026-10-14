package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.PageResult;
import com.gss.dto.TractorDTO;
import com.gss.entity.Tractor;
import com.gss.mapper.TractorMapper;
import com.gss.vo.TractorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 拖车服务
 */
@Service
@RequiredArgsConstructor
public class TractorService {

    private final TractorMapper tractorMapper;

    /**
     * 分页查询拖车。
     * 数据权限：companyId 由 Controller 根据 JWT 身份传入（COMPANY 强制本公司）
     */
    public PageResult<TractorVO> page(Integer page, Integer size,
                                      String plateNo, String icCardNo, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<TractorVO> rows = tractorMapper.selectPage(plateNo, icCardNo, companyId, (p - 1) * s, s);
        Long total = tractorMapper.countPage(plateNo, icCardNo, companyId);
        return PageResult.of(total, rows);
    }

    public Tractor getById(Long id) {
        Tractor tractor = tractorMapper.selectById(id);
        if (tractor == null) {
            throw new BusinessException("拖车不存在");
        }
        return tractor;
    }

    public void create(TractorDTO dto) {
        Tractor tractor = new Tractor();
        copy(tractor, dto);
        tractor.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        tractorMapper.insert(tractor);
    }

    public void update(Long id, TractorDTO dto) {
        Tractor tractor = getById(id);
        copy(tractor, dto);
        if (dto.getStatus() != null) {
            tractor.setStatus(dto.getStatus());
        }
        tractorMapper.update(tractor);
    }

    public void delete(Long id) {
        getById(id);
        tractorMapper.deleteById(id);
    }

    private void copy(Tractor tractor, TractorDTO dto) {
        tractor.setPlateNo(dto.getPlateNo());
        tractor.setFrameNo(dto.getFrameNo());
        tractor.setIcCardNo(dto.getIcCardNo());
        tractor.setCompanyId(dto.getCompanyId());
        tractor.setAxleNum(dto.getAxleNum() == null ? 4 : dto.getAxleNum());
    }
}
