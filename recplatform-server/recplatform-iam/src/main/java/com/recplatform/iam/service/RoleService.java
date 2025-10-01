package com.recplatform.iam.service;

import com.recplatform.common.model.PageResult;
import com.recplatform.iam.dto.RoleCreateRequest;
import com.recplatform.iam.dto.RoleUpdateRequest;
import com.recplatform.iam.entity.PermissionEntity;
import com.recplatform.iam.vo.RoleVO;

import java.util.List;
import java.util.Set;

/**
 * Role service.
 */
public interface RoleService {

    /**
     * Create a new role.
     */
    RoleVO createRole(RoleCreateRequest request);

    /**
     * Get role by ID.
     */
    RoleVO getRole(Long id);

    /**
     * List roles with pagination.
     */
    PageResult<RoleVO> listRoles(String name, Integer page, Integer size);

    /**
     * Update role.
     */
    RoleVO updateRole(Long id, RoleUpdateRequest request);

    /**
     * Delete role.
     */
    void deleteRole(Long id);

    /**
     * Assign permissions to role.
     */
    void assignPermissions(Long roleId, Set<String> permissionCodes);

    /**
     * Get role permissions.
     */
    List<PermissionEntity> getRolePermissions(Long roleId);
}
