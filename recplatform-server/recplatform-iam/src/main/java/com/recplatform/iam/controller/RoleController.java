package com.recplatform.iam.controller;

import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.iam.dto.PermissionAssignRequest;
import com.recplatform.iam.dto.RoleCreateRequest;
import com.recplatform.iam.dto.RoleUpdateRequest;
import com.recplatform.iam.service.RoleService;
import com.recplatform.iam.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;

/**
 * Role management controller.
 */
@RestController
@RequestMapping("/api/v1/iam/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role CRUD APIs")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Create role")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleService.createRole(request));
    }

    @GetMapping
    @Operation(summary = "List roles with pagination")
    public Result<PageResult<RoleVO>> listRoles(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(roleService.listRoles(name, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role with permissions")
    public Result<RoleVO> getRole(@PathVariable Long id) {
        return Result.success(roleService.getRole(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role")
    public Result<RoleVO> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        return Result.success(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Assign permissions to role")
    public Result<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody PermissionAssignRequest request) {
        roleService.assignPermissions(id, new HashSet<>(request.getPermissionCodes()));
        return Result.success();
    }
}
