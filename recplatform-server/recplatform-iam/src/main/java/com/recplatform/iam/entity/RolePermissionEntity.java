package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role-Permission association entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class RolePermissionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    private String permissionCode;
}
