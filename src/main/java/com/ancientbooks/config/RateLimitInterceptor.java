package com.ancientbooks.config;

import com.ancientbooks.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器
 * 基于 Redis 实现接口限流，防止 API 滥用
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.max-requests:30}")
    private int maxRequests;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${rate-limit.prefix:rate:limit:}")
    private String prefix;

    public RateLimitInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {
        // 限流未开启则放行
        if (!enabled) {
            return true;
        }

        // 白名单路径放行
        String path = request.getRequestURI();
        if (isWhitelistPath(path)) {
            return true;
        }

        // 获取用户ID（匿名用户用 IP）
        String key;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            key = prefix + "user:" + principal.getUserId();
        } else {
            // 匿名用户用 IP 限流
            String ip = getClientIP(request);
            key = prefix + "ip:" + ip;
        }

        // 使用 Redis INCR 实现原子递增
        Long count = redisTemplate.opsForValue().increment(key);

        // 首次访问设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        // 超出限制
        if (count != null && count > maxRequests) {
            log.warn("请求过于频繁，IP：{}, Path：{}, Count：{}", key, path, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            try (var writer = response.getWriter()) {
                writer.write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}");
            } catch (IOException e) {
                log.error("写入响应失败", e);
            }
            return false;
        }

        // 添加响应头
        response.addHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(maxRequests - count));
        response.addHeader("X-RateLimit-Reset", String.valueOf(windowSeconds));

        return true;
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

    /**
     * 获取客户端 IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
