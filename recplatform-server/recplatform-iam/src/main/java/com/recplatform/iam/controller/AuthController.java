package com.recplatform.iam.controller;

import com.recplatform.common.result.Result;
import com.recplatform.iam.dto.LoginRequest;
import com.recplatform.iam.dto.LoginResponse;
import com.recplatform.iam.service.AuthService;
import com.recplatform.iam.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login with username and password to get token")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout and invalidate token")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/userinfo")
    @Operation(summary = "Get current user", description = "Get current logged-in user info")
    public Result<UserVO> getUserInfo() {
        return Result.success(authService.getCurrentUser());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh authentication token")
    public Result<LoginResponse> refresh() {
        return Result.success(authService.refreshToken());
    }
}
