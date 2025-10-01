package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User-Role association entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class UserRoleEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long roleId;
}
