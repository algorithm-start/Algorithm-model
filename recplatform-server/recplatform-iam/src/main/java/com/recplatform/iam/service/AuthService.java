package com.recplatform.iam.service;

import com.recplatform.iam.dto.LoginRequest;
import com.recplatform.iam.dto.LoginResponse;
import com.recplatform.iam.vo.UserVO;

/**
 * Authentication service.
 */
public interface AuthService {

    /**
     * Login with username and password.
     */
    LoginResponse login(LoginRequest request);

    /**
     * Logout current user.
     */
    void logout();

    /**
     * Get current logged-in user info.
     */
    UserVO getCurrentUser();

    /**
     * Refresh token.
     */
    LoginResponse refreshToken();
}
