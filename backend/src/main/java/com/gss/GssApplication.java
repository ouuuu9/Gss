package com.gss;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 集装箱码头闸口预约与办单管理系统 启动类
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.gss.mapper")
@EnableScheduling
public class GssApplication {

    public static void main(String[] args) {
        SpringApplication.run(GssApplication.class, args);
        log.info("====== GSS 后端启动成功，接口地址 http://localhost:8080/api ======");
    }
}
