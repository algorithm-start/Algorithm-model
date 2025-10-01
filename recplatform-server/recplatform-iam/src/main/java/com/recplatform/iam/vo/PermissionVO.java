package com.recplatform.iam.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Permission view object.
 */
@Data
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String module;

    private String description;

    private String type;

    private LocalDateTime createTime;

    /**
     * Child permissions for tree structure.
     */
    private List<PermissionVO> children;
}
