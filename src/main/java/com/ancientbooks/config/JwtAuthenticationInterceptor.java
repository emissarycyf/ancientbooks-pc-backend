package com.ancientbooks.config;

import com.ancientbooks.security.JwtTokenProvider;
import com.ancientbooks.security.UserPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证拦截器
 * 在请求到达 Controller 之前校验 Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NullMarked
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request,
                            @Nullable HttpServletResponse response,
                            @Nullable Object handler) throws Exception {
        // 白名单路径放行
        String path = request.getRequestURI();
        if (isWhitelistPath(path)) {
            return true;
        }

        // 从请求头获取 Token
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("未提供 Token，请求路径：{}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try (var writer = response.getWriter()) {
                writer.write("{\"code\":401,\"msg\":\"未登录，请先登录\"}");
            } catch (IOException e) {
                log.error("写入响应失败", e);
            }
            return false;
        }

        // 提取 Token
        String token = header.substring(7);

        try {
            // 解析 Token 获取用户ID
            String userId = tokenProvider.extractUserId(token);

            // 检查 Token 是否在黑名单（登出时加入）
            // TODO: 集成 RedisTemplate 检查黑名单

            // 设置用户信息到 SecurityContext
            UserPrincipal principal = new UserPrincipal(Long.parseLong(userId), Collections.emptyList());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token 无效或已过期，请求路径：{}，错误：{}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try (var writer = response.getWriter()) {
                writer.write("{\"code\":401,\"msg\":\"Token 无效或已过期\"}");
            } catch (IOException ioException) {
                log.error("写入响应失败", ioException);
            }
            return false;
        }
    }

    /**
     * 白名单路径
     */
    private boolean isWhitelistPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/public/")
                || path.startsWith("/doc.html")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }
}
