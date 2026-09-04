package com.ancientbooks.service.impl;

import com.ancientbooks.dto.LoginRequest;
import com.ancientbooks.dto.LoginResponse;
import com.ancientbooks.entity.User;
import com.ancientbooks.mapper.UserMapper;
import com.ancientbooks.security.JwtTokenProvider;
import com.ancientbooks.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = this.getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0));

        if (user == null) {
            throw new com.ancientbooks.exception.BusinessException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new com.ancientbooks.exception.BusinessException("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() != 1) {
            throw new com.ancientbooks.exception.BusinessException("账户已被禁用");
        }

        // 4. 生成 JWT
        String token = tokenProvider.generateToken(user.getId().toString(), null);

        // 5. 返回结果
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }
}
