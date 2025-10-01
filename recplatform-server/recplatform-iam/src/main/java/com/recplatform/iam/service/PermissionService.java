package com.recplatform.iam.service;

import com.recplatform.iam.vo.PermissionVO;

import java.util.List;

/**
 * Permission service.
 */
public interface PermissionService {

    /**
     * List all permissions.
     */
    List<PermissionVO> listAll();

    /**
     * Get permission tree grouped by module.
     */
    List<PermissionVO> getPermissionTree();
}
