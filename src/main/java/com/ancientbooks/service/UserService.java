package com.ancientbooks.service;

import com.ancientbooks.dto.LoginRequest;
import com.ancientbooks.dto.LoginResponse;
import com.ancientbooks.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    LoginResponse login(LoginRequest request);
}
