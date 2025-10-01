package com.recplatform.iam.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.iam.dto.LoginRequest;
import com.recplatform.iam.dto.LoginResponse;
import com.recplatform.iam.entity.UserEntity;
import com.recplatform.iam.mapper.UserMapper;
import com.recplatform.iam.service.AuthService;
import com.recplatform.iam.service.UserService;
import com.recplatform.iam.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Authentication service implementation using Sa-Token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user by username
        UserEntity user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BusinessException(ResultCode.AUTH_FAILED, "Invalid username or password");
        }

        // Check user status
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if ("LOCKED".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED, "Account is locked");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.AUTH_FAILED, "Invalid username or password");
        }

        // Sa-Token login
        StpUtil.login(user.getId());
        // Store username in session for audit logging
        StpUtil.getSession().set("username", user.getUsername());

        // Update last login info
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Build response
        UserVO userVO = userService.getUser(user.getId());
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .tokenType("Bearer")
                .expiresIn(StpUtil.getTokenTimeout())
                .user(userVO)
                .build();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserVO getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        return userService.getUser(userId);
    }

    @Override
    public LoginResponse refreshToken() {
        // Sa-Token handles token refresh automatically
        // Just return the current token info
        long userId = StpUtil.getLoginIdAsLong();
        UserVO userVO = userService.getUser(userId);
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .tokenType("Bearer")
                .expiresIn(StpUtil.getTokenTimeout())
                .user(userVO)
                .build();
    }
}
