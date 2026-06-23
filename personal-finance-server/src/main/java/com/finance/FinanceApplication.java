package com.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 个人智能理财系统 - 后端启动类
 *
 * @author 胡宪棋
 * @date 2026-06-23
 */
@SpringBootApplication
@MapperScan("com.finance.mapper")
@EnableAsync
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  个人智能理财系统后端服务启动成功！");
        System.out.println("  Knife4j文档: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}
