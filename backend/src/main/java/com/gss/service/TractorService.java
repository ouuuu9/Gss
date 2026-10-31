package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.common.Constants;
import com.gss.common.PageResult;
import com.gss.dto.TractorDTO;
import com.gss.entity.Tractor;
import com.gss.entity.TransportCompany;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.vo.TractorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 拖车服务
 */
@Service
@RequiredArgsConstructor
public class TractorService {

    private final TractorMapper tractorMapper;
    private final TransportCompanyMapper companyMapper;

    /**
     * 分页查询拖车。
     * 数据权限：companyId 由 Controller 根据 JWT 身份传入（COMPANY 强制本公司）
     */
    @Transactional(readOnly = true)
    public PageResult<TractorVO> page(Integer page, Integer size,
                                      String plateNo, String icCardNo, Long companyId) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        List<TractorVO> rows = tractorMapper.selectPage(plateNo, icCardNo, companyId, (p - 1) * s, s);
        Long total = tractorMapper.countPage(plateNo, icCardNo, companyId);
        return PageResult.of(total, rows);
    }

    @Transactional(readOnly = true)
    public Tractor getById(Long id) {
        Tractor tractor = tractorMapper.selectById(id);
        if (tractor == null) {
            throw new BusinessException("拖车不存在");
        }
        checkOwnership(tractor);
        return tractor;
    }

    public void create(TractorDTO dto) {
        validateCompany(dto.getCompanyId());
        Tractor tractor = new Tractor();
        copy(tractor, dto);
        tractor.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        tractorMapper.insert(tractor);
    }

    public void update(Long id, TractorDTO dto) {
        Tractor tractor = getById(id);
        validateCompany(dto.getCompanyId());
        copy(tractor, dto);
        if (dto.getStatus() != null) {
            tractor.setStatus(dto.getStatus());
        }
        tractorMapper.update(tractor);
    }

    /**
     * 删除拖车。
     * 有进行中单证 → 拒绝；仅有历史单证（已完成/已作废）→ 软删除（置为停用），
     * 保留历史单证的车牌号展示；无任何单证引用 → 物理删除。
     *
     * @return true=物理删除，false=软删除（已停用）
     */
    public boolean delete(Long id) {
        getById(id);
        if (tractorMapper.hasActiveEir(id) == 1) {
            throw new BusinessException("该拖车仍有关联的进行中单证（已派单/已预约/已进闸），无法删除");
        }
        if (tractorMapper.hasEirHistory(id) == 1) {
            Tractor tractor = new Tractor();
            tractor.setId(id);
            tractor.setStatus(0);
            tractorMapper.update(tractor);
            return false;
        }
        tractorMapper.deleteById(id);
        return true;
    }

    /**
     * 校验所属运输公司真实存在，防止产生孤儿拖车记录（无 FK 约束，应用层兜底）
     */
    private void validateCompany(Long companyId) {
        TransportCompany company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException("所属运输公司不存在");
        }
    }

    /**
     * 数据权限：COMPANY 只能查看/修改/删除本公司拖车，防止按 ID 越权操作他公司档案
     */
    private void checkOwnership(Tractor tractor) {
        LoginUser user = UserContext.require();
        if (Constants.ROLE_COMPANY.equals(user.getRole())
                && !tractor.getCompanyId().equals(user.getCompanyId())) {
            throw new BusinessException("无权操作其他公司的拖车");
        }
    }

    private void copy(Tractor tractor, TractorDTO dto) {
        tractor.setPlateNo(dto.getPlateNo());
        tractor.setFrameNo(dto.getFrameNo());
        tractor.setIcCardNo(dto.getIcCardNo());
        tractor.setCompanyId(dto.getCompanyId());

        // 轴数未填写时按默认 4 轴处理
        if (dto.getAxleNum() == null) {
            tractor.setAxleNum(4);
        } else {
            tractor.setAxleNum(dto.getAxleNum());
        }
    }
}
