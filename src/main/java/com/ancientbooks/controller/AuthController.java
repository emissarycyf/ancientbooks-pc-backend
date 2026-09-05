package com.ancientbooks.controller;

import com.ancientbooks.dto.LoginRequest;
import com.ancientbooks.dto.LoginResponse;
import com.ancientbooks.dto.Result;
import com.ancientbooks.security.JwtTokenProvider;
import com.ancientbooks.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 * 登录、注册、登出
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // ✅ 安全修复：不记录密码信息
        log.info("用户登录：{}", maskSensitiveInfo(request.getUsername()));
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     * ✅ 安全修复：将Token加入黑名单，立即失效
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求头获取Token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // 解析Token获取过期时间
                Claims claims = tokenProvider.extractAllClaims(token);
                Date expiration = claims.getExpiration();

                // 计算Token剩余有效时间
                long ttl = expiration.getTime() - System.currentTimeMillis();

                // 如果Token还未过期，加入黑名单
                if (ttl > 0) {
                    String blacklistKey = "jwt:blacklist:" + token;
                    // 设置过期时间，与Token原过期时间一致
                    redisTemplate.opsForValue().set(blacklistKey, "logout", ttl, TimeUnit.MILLISECONDS);
                    log.info("Token已加入黑名单，剩余有效时间：{}ms", ttl);
                }

            } catch (Exception e) {
                log.warn("处理登出时解析Token失败", e);
            }
        }

        // 清除SecurityContext
        SecurityContextHolder.clearContext();
        return Result.success();
    }

    /**
     * 刷新 Token
     * ✅ 安全修复：实现Token刷新逻辑
     */
    @PostMapping("/refresh")
    public Result<String> refreshToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Result.error(401, "未提供Token");
        }

        String token = header.substring(7);

        try {
            // 解析旧Token
            String userId = tokenProvider.extractUserId(token);

            // ✅ 检查是否在黑名单
            String blacklistKey = tokenProvider.getBlacklistKey(token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                return Result.error(401, "Token已失效");
            }

            // 生成新Token
            String newToken = tokenProvider.generateToken(userId, null);

            // 将旧Token加入黑名单
            Claims claims = tokenProvider.extractAllClaims(token);
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "refresh", ttl, TimeUnit.MILLISECONDS);
            }

            log.info("Token刷新成功，用户ID：{}", maskSensitiveInfo(userId));
            return Result.success(newToken);

        } catch (Exception e) {
            log.error("Token刷新失败", e);
            return Result.error(401, "Token刷新失败");
        }
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() < 3) {
            return "***";
        }
        return info.substring(0, 2) + "***";
    }
}
