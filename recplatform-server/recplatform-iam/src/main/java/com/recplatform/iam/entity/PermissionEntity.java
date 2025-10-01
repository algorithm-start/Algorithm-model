package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * System permission entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class PermissionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Permission code, e.g., "solver:problem:create"
     */
    private String code;

    private String name;

    private String module;

    private String description;

    /**
     * Permission type: MENU, BUTTON, API
     */
    private String type;
}
