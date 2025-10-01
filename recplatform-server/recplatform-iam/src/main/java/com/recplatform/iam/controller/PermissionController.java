package com.recplatform.iam.controller;

import com.recplatform.common.result.Result;
import com.recplatform.iam.service.PermissionService;
import com.recplatform.iam.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Permission controller.
 */
@RestController
@RequestMapping("/api/v1/iam/permissions")
@RequiredArgsConstructor
@Tag(name = "Permission Management", description = "Permission query APIs")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "List all permissions")
    public Result<List<PermissionVO>> listAll() {
        return Result.success(permissionService.listAll());
    }

    @GetMapping("/tree")
    @Operation(summary = "Get permission tree grouped by module")
    public Result<List<PermissionVO>> getPermissionTree() {
        return Result.success(permissionService.getPermissionTree());
    }
}
