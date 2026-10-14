package com.gss.config;

import com.gss.entity.Driver;
import com.gss.entity.SysUser;
import com.gss.mapper.DriverMapper;
import com.gss.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时初始化系统账号：
 * - 系统内无任何用户时，创建默认的码头端/公司端账号，并为每位司机创建登录账号
 * - 默认密码统一为 123456（BCrypt 加密存储）
 * - 已存在用户数据时不做任何处理，避免覆盖业务数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final DriverMapper driverMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // 幂等补全：逐个检查默认账号，不存在则创建。
        // 即使 SQL 已预置用户也能安全运行，不会覆盖已有账号。

        // 码头端账号
        createIfAbsent("admin", "系统管理员", "ADMIN", null, null);
        createIfAbsent("gate", "闸口办单员", "GATE", null, null);

        // 运输公司账号
        createIfAbsent("company1", "第一运输账号", "COMPANY", 1L, null);
        createIfAbsent("company2", "第二运输账号", "COMPANY", 2L, null);

        // 司机账号：用户名 = 手机号
        List<Driver> drivers = driverMapper.selectAll();
        if (drivers != null) {
            for (Driver driver : drivers) {
                createIfAbsent(driver.getPhone(), driver.getName(), "DRIVER",
                        driver.getCompanyId(), driver.getId());
            }
        }

        log.info("====== 默认账号检查完成，所有默认账号密码均为：{} ======", DEFAULT_PASSWORD);
    }

    /**
     * 若用户不存在则创建，已存在则跳过（不覆盖已有密码与数据）
     */
    private void createIfAbsent(String username, String name, String role, Long companyId, Long driverId) {
        SysUser existing = sysUserMapper.selectByUsername(username);
        if (existing != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setName(name);
        user.setRole(role);
        user.setCompanyId(companyId);
        user.setDriverId(driverId);
        user.setStatus(1);
        sysUserMapper.insert(user);
        log.info("创建默认账号：{} ({})", username, role);
    }
}
