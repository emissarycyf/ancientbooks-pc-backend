package com.ancientbooks.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 安全配置
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private final com.ancientbooks.config.JwtAuthenticationInterceptor jwtInterceptor;
    private final com.ancientbooks.config.RateLimitInterceptor rateLimitInterceptor;

    // 构造函数注入
    public SecurityConfig(JwtAuthenticationInterceptor jwtInterceptor,
                         com.ancientbooks.config.RateLimitInterceptor rateLimitInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 认证拦截器（优先级高）
        registry.addInterceptor(jwtInterceptor)
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/public/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        // 限流拦截器（优先级低）
        registry.addInterceptor(rateLimitInterceptor)
                .order(2)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/public/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
