package com.howie.usermatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 * <p>
 * 前端运行在 localhost:5173，后端在 localhost:8080，属于跨域请求；
 * 登录态使用 Session Cookie，必须允许携带凭证（allowCredentials），
 * 因此 origin 不能是 *，必须明确列出。
 * <p>
 * 允许的来源通过环境变量 CORS_ALLOWED_ORIGINS 注入（逗号分隔），
 * 切换开发 / 测试 / 生产环境时无需改动代码。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许携带 Cookie 时，origin 不能是 *，必须明确指定来源
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
