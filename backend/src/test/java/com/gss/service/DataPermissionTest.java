package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.dto.UserDTO;
import com.gss.entity.Driver;
import com.gss.entity.EirOrder;
import com.gss.entity.FeeRecord;
import com.gss.entity.SysUser;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.EirOrderMapper;
import com.gss.mapper.FeeRecordMapper;
import com.gss.mapper.SysUserMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import com.gss.statemachine.EirStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 数据权限单元测试（纯 Mockito，不依赖 Spring 与数据库）。
 * 覆盖：跨公司按 ID 改删司机、取消派单删除已缴费记录、删除自身/最后管理员、
 * 司机账号缺司机档案四类越权与一致性防护。
 */
@ExtendWith(MockitoExtension.class)
class DataPermissionTest {

    @Mock
    private DriverMapper driverMapper;
    @Mock
    private TractorMapper tractorMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private TransportCompanyMapper companyMapper;
    @Mock
    private EirOrderMapper eirMapper;
    @Mock
    private FeeRecordMapper feeMapper;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private LoginUser company(long companyId) {
        return LoginUser.builder().userId(100L).role("COMPANY").companyId(companyId).build();
    }

    /** company2 删除 company1 的司机应被拒绝 */
    @Test
    void companyCannotDeleteOtherCompanyDriver() {
        Driver other = new Driver();
        other.setId(1L);
        other.setCompanyId(1L);
        when(driverMapper.selectById(1L)).thenReturn(other);
        UserContext.set(company(2L));

        DriverService service = new DriverService(driverMapper, tractorMapper, companyMapper);
        assertThrows(BusinessException.class, () -> service.delete(1L));
    }

    /** company2 修改 company1 的司机应被拒绝 */
    @Test
    void companyCannotUpdateOtherCompanyDriver() {
        Driver other = new Driver();
        other.setId(1L);
        other.setCompanyId(1L);
        when(driverMapper.selectById(1L)).thenReturn(other);
        UserContext.set(company(2L));

        DriverService service = new DriverService(driverMapper, tractorMapper, companyMapper);
        assertThrows(BusinessException.class, () -> service.update(1L, new com.gss.dto.DriverDTO()));
    }

    /** 已缴费单证取消派单应被拒绝（防止已收费用记录被删除） */
    @Test
    void cancelDispatchRejectsPaidFee() {
        EirOrder eir = new EirOrder();
        eir.setId(9L);
        eir.setStatus("DISPATCHED");
        eir.setCompanyId(2L);
        when(eirMapper.selectById(9L)).thenReturn(eir);
        when(eirMapper.updateStatus(any(EirOrder.class))).thenReturn(1);
        FeeRecord paid = new FeeRecord();
        paid.setPayStatus(1);
        when(feeMapper.selectByEirId(9L)).thenReturn(paid);

        EirService service = new EirService(eirMapper, null, null, null, feeMapper, null, null, new EirStateMachine());
        assertThrows(BusinessException.class, () -> service.cancelDispatch(9L, company(2L)));
    }

    /** 管理员不能删除当前登录的自身账号 */
    @Test
    void adminCannotDeleteSelf() {
        SysUser me = new SysUser();
        me.setId(5L);
        me.setRole("ADMIN");
        when(sysUserMapper.selectById(5L)).thenReturn(me);
        UserContext.set(LoginUser.builder().userId(5L).role("ADMIN").build());

        SysUserService service = new SysUserService(sysUserMapper, new BCryptPasswordEncoder(), driverMapper, companyMapper);
        assertThrows(BusinessException.class, () -> service.delete(5L));
    }

    /** 删除最后一个管理员应被拒绝 */
    @Test
    void cannotDeleteLastAdmin() {
        SysUser target = new SysUser();
        target.setId(6L);
        target.setRole("ADMIN");
        when(sysUserMapper.selectById(6L)).thenReturn(target);
        when(sysUserMapper.countPage(null, "ADMIN", null)).thenReturn(1L);
        UserContext.set(LoginUser.builder().userId(5L).role("ADMIN").build());

        SysUserService service = new SysUserService(sysUserMapper, new BCryptPasswordEncoder(), driverMapper, companyMapper);
        assertThrows(BusinessException.class, () -> service.delete(6L));
    }

    /** 司机账号不关联司机档案应被拒绝（否则预约时空指针） */
    @Test
    void driverAccountRequiresDriverId() {
        when(sysUserMapper.selectByUsername(anyString())).thenReturn(null);
        UserDTO dto = new UserDTO();
        dto.setUsername("driver-x");
        dto.setPassword("123456");
        dto.setRole("DRIVER");
        dto.setCompanyId(2L);

        SysUserService service = new SysUserService(sysUserMapper, new BCryptPasswordEncoder(), driverMapper, companyMapper);
        assertThrows(BusinessException.class, () -> service.create(dto));
    }
}
