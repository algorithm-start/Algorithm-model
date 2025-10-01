package com.recplatform.iam.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User view object (no password).
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private String status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<RoleVO> roles;

    /**
     * Merged menu keys from all of the user's roles, filtered by platform
     * capability status. The front-end sidebar renders only these menus.
     */
    private List<String> menus;
}
