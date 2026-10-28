package com.gss.service;

import com.gss.common.BusinessException;
import com.gss.entity.FeeRecord;
import com.gss.mapper.FeeRecordMapper;
import com.gss.security.LoginUser;
import com.gss.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收费服务单元测试（纯 Mockito，不依赖 Spring 与数据库）。
 * 覆盖：跨公司缴费拒绝、已缴费重复缴费拒绝、批量缴费过滤他公司+已缴费记录。
 */
@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock
    private FeeRecordMapper feeMapper;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private LoginUser company(long companyId) {
        return LoginUser.builder().userId(100L).role("COMPANY")
                .companyId(companyId).name("张三").build();
    }

    /** 跨公司缴费应被拒绝（只能缴纳本公司费用） */
    @Test
    void crossCompanyPayRejected() {
        FeeRecord fee = new FeeRecord();
        fee.setId(1L);
        fee.setCompanyId(1L);
        fee.setPayStatus(0);
        when(feeMapper.selectById(1L)).thenReturn(fee);
        UserContext.set(company(2L));

        FeeService service = new FeeService(feeMapper);
        assertThrows(BusinessException.class, () -> service.pay(1L));
    }

    /** 已缴费记录再次 pay 应被拒绝（幂等，防重复扣款） */
    @Test
    void payAlreadyPaidRejected() {
        FeeRecord fee = new FeeRecord();
        fee.setId(1L);
        fee.setCompanyId(2L);
        fee.setPayStatus(1);  // 已缴费
        when(feeMapper.selectById(1L)).thenReturn(fee);
        UserContext.set(company(2L));

        FeeService service = new FeeService(feeMapper);
        assertThrows(BusinessException.class, () -> service.pay(1L));
    }

    /** 批量缴费：过滤掉他公司记录和已缴费记录，只更新本公司未缴费的 */
    @Test
    void batchPayFiltersNonOwnAndPaid() {
        // 构造 4 条记录：2 条本公司未缴费（应处理）、1 条他公司（应跳过）、1 条本公司已缴费（应跳过）
        FeeRecord ownUnpaid = new FeeRecord();
        ownUnpaid.setId(1L); ownUnpaid.setCompanyId(2L); ownUnpaid.setPayStatus(0);

        FeeRecord ownUnpaid2 = new FeeRecord();
        ownUnpaid2.setId(2L); ownUnpaid2.setCompanyId(2L); ownUnpaid2.setPayStatus(null);  // null 视为未缴费

        FeeRecord otherCompany = new FeeRecord();
        otherCompany.setId(3L); otherCompany.setCompanyId(99L); otherCompany.setPayStatus(0);

        FeeRecord ownPaid = new FeeRecord();
        ownPaid.setId(4L); ownPaid.setCompanyId(2L); ownPaid.setPayStatus(1);

        when(feeMapper.selectByIds(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(ownUnpaid, ownUnpaid2, otherCompany, ownPaid));
        UserContext.set(company(2L));

        FeeService service = new FeeService(feeMapper);
        int count = service.batchPay(List.of(1L, 2L, 3L, 4L));

        assertEquals(2, count);
        verify(feeMapper).markPaidBatch(List.of(1L, 2L), "张三");
    }
}
