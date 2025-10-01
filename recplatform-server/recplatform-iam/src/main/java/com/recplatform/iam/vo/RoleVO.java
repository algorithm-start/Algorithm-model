package com.recplatform.iam.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Role view object.
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String description;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<String> permissionCodes;

    /**
     * Menu keys visible to this role.
     */
    private List<String> menus;
}
