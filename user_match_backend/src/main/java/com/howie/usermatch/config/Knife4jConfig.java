package com.howie.usermatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Knife4j 接口文档配置
 */
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean
    public Docket defaultApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描 Controller 包路径
                .apis(RequestHandlerSelectors.basePackage("com.howie.usermatch.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * API 基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("用户匹配系统接口文档")
                .description("用户匹配系统后端接口")
                .contact(new Contact("howie", "", ""))
                .version("1.0.0")
                .build();
    }
}
