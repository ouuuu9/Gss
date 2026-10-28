package com.gss.service;

import com.gss.entity.Driver;
import com.gss.entity.Tractor;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.TractorMapper;
import com.gss.mapper.TransportCompanyMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 司机/拖车删除的软删除策略单元测试（纯 Mockito，不依赖 Spring 与数据库）。
 * 规则：有进行中单证 → 拒绝删除（DataPermissionTest 已覆盖归属拒绝）；
 * 仅有历史单证（已完成/已作废）→ 软删除（置为停用）保留追溯；
 * 无任何单证引用 → 物理删除。
 */
@ExtendWith(MockitoExtension.class)
class SoftDeleteTest {

    @Mock
    private DriverMapper driverMapper;
    @Mock
    private TractorMapper tractorMapper;
    @Mock
    private TransportCompanyMapper companyMapper;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void loginAsCompany(long companyId) {
        UserContext.set(LoginUser.builder().userId(100L).role("COMPANY").companyId(companyId).build());
    }

    /** 有历史单证的司机删除时应改为停用，不物理删除 */
    @Test
    void driverWithEirHistoryIsSoftDeleted() {
        Driver driver = new Driver();
        driver.setId(1L);
        driver.setCompanyId(1L);
        when(driverMapper.selectById(1L)).thenReturn(driver);
        when(driverMapper.hasActiveEir(1L)).thenReturn(0);
        when(driverMapper.hasEirHistory(1L)).thenReturn(1);
        loginAsCompany(1L);

        DriverService service = new DriverService(driverMapper, tractorMapper, companyMapper);
        boolean removed = service.delete(1L);

        assertFalse(removed);
        verify(driverMapper).update(argThat(d -> d.getId().equals(1L) && Integer.valueOf(0).equals(d.getStatus())));
        verify(driverMapper, never()).deleteById(1L);
    }

    /** 无任何单证引用的司机删除时应物理删除 */
    @Test
    void driverWithoutAnyEirIsPhysicallyDeleted() {
        Driver driver = new Driver();
        driver.setId(1L);
        driver.setCompanyId(1L);
        when(driverMapper.selectById(1L)).thenReturn(driver);
        when(driverMapper.hasActiveEir(1L)).thenReturn(0);
        when(driverMapper.hasEirHistory(1L)).thenReturn(0);
        loginAsCompany(1L);

        DriverService service = new DriverService(driverMapper, tractorMapper, companyMapper);
        boolean removed = service.delete(1L);

        assertTrue(removed);
        verify(driverMapper).deleteById(1L);
        verify(driverMapper, never()).update(argThat(d -> d.getId().equals(1L)));
    }

    /** 有历史单证的拖车删除时应改为停用，不物理删除 */
    @Test
    void tractorWithEirHistoryIsSoftDeleted() {
        Tractor tractor = new Tractor();
        tractor.setId(2L);
        tractor.setCompanyId(1L);
        when(tractorMapper.selectById(2L)).thenReturn(tractor);
        when(tractorMapper.hasActiveEir(2L)).thenReturn(0);
        when(tractorMapper.hasEirHistory(2L)).thenReturn(1);
        loginAsCompany(1L);

        TractorService service = new TractorService(tractorMapper, companyMapper);
        boolean removed = service.delete(2L);

        assertFalse(removed);
        verify(tractorMapper).update(argThat(t -> t.getId().equals(2L) && Integer.valueOf(0).equals(t.getStatus())));
        verify(tractorMapper, never()).deleteById(2L);
    }

    /** 无任何单证引用的拖车删除时应物理删除 */
    @Test
    void tractorWithoutAnyEirIsPhysicallyDeleted() {
        Tractor tractor = new Tractor();
        tractor.setId(2L);
        tractor.setCompanyId(1L);
        when(tractorMapper.selectById(2L)).thenReturn(tractor);
        when(tractorMapper.hasActiveEir(2L)).thenReturn(0);
        when(tractorMapper.hasEirHistory(2L)).thenReturn(0);
        loginAsCompany(1L);

        TractorService service = new TractorService(tractorMapper, companyMapper);
        boolean removed = service.delete(2L);

        assertTrue(removed);
        verify(tractorMapper).deleteById(2L);
        verify(tractorMapper, never()).update(argThat(t -> t.getId().equals(2L)));
    }
}
