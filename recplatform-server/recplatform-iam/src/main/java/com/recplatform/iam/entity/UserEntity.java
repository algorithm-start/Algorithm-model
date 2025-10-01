package com.recplatform.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System user entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /**
     * User status: ACTIVE, DISABLED, LOCKED
     */
    private String status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;
}
