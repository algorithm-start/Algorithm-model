package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * System role entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class RoleEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String code;

    private String name;

    private String description;

    /**
     * Role status: ACTIVE, DISABLED
     */
    private String status;

    /**
     * JSON array of menu keys visible to this role, e.g.
     * {@code ["dashboard","solver","data","admin"]}.
     */
    private String menus;
}
