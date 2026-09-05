package com.ancientbooks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // ✅ 修复：使用强度12（默认10），增加破解难度
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离使用JWT，不需要CSRF）
            .csrf(csrf -> csrf.disable())

            // 设置会话为无状态（JWT 不需要 Session）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 授权配置
            .authorizeHttpRequests(auth -> auth
                // 公开接口：认证相关
                .requestMatchers("/api/auth/**").permitAll()
                // 公开接口：静态资源、文档
                .requestMatchers("/public/**", "/doc.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 需要认证的接口：古籍分析
                .requestMatchers("/api/agent/**").authenticated()
                // 需要认证的接口：对话历史
                .requestMatchers("/api/chat-history/**").authenticated()
                // 其他接口
                .anyRequest().permitAll()
            );

        // JWT认证拦截器已通过 WebMvcConfig 注册

        return http.build();
    }
}

