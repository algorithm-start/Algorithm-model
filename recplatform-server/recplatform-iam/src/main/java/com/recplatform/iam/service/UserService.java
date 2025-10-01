package com.recplatform.iam.service;

import com.recplatform.common.model.PageResult;
import com.recplatform.iam.dto.UserCreateRequest;
import com.recplatform.iam.dto.UserUpdateRequest;
import com.recplatform.iam.vo.UserVO;

import java.util.Set;

/**
 * User service.
 */
public interface UserService {

    /**
     * Create a new user.
     */
    UserVO createUser(UserCreateRequest request);

    /**
     * Get user by ID.
     */
    UserVO getUser(Long id);

    /**
     * List users with pagination.
     */
    PageResult<UserVO> listUsers(String username, String status, Integer page, Integer size);

    /**
     * Update user.
     */
    UserVO updateUser(Long id, UserUpdateRequest request);

    /**
     * Delete (disable) user.
     */
    void deleteUser(Long id);

    /**
     * Change user password.
     */
    void changePassword(Long id, String newPassword);

    /**
     * Assign roles to user.
     */
    void assignRoles(Long userId, Set<Long> roleIds);

    /**
     * Get user permissions aggregated from all roles.
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * Get user entity by username.
     */
    UserVO getUserByUsername(String username);
}
