package com.ancientbooks.controller;

import com.ancientbooks.dto.LoginRequest;
import com.ancientbooks.dto.LoginResponse;
import com.ancientbooks.dto.Result;
import com.ancientbooks.entity.User;
import com.ancientbooks.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录：{}", request.getUsername());
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // TODO: 将 Token 加入黑名单
        return Result.success();
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authHeader) {
        // TODO: 刷新 Token 逻辑
        return Result.success("refresh-token");
    }
}
