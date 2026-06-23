package com.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * @author 胡宪棋
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人智能理财系统接口文档")
                        .version("V1.0")
                        .description("SpringBoot后端RESTful API — 支持鸿蒙ArkTS客户端 & Vue3管理员后台\n\n"
                                + "作者：胡宪棋 | 班级：软件2413 | 学号：202421332084")
                        .contact(new Contact()
                                .name("胡宪棋")
                                .url("http://localhost:8080/doc.html")));
    }
}
