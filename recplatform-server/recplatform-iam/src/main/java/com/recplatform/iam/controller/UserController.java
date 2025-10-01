package com.recplatform.iam.controller;

import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.iam.dto.*;
import com.recplatform.iam.service.UserService;
import com.recplatform.iam.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;

/**
 * User management controller.
 */
@RestController
@RequestMapping("/api/v1/iam/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD APIs")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create user")
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    @GetMapping
    @Operation(summary = "List users with pagination")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(userService.listUsers(username, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUser(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public Result<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete/disable user")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Change user password")
    public Result<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request.getNewPassword());
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign roles to user")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody RoleAssignRequest request) {
        userService.assignRoles(id, new HashSet<>(request.getRoleIds()));
        return Result.success();
    }
}
