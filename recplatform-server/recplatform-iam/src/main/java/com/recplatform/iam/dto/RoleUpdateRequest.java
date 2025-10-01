package com.recplatform.iam.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Role update request DTO.
 */
@Data
public class RoleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    private String status;

    /**
     * Menu keys visible to this role (e.g. ["dashboard","solver","data"]).
     */
    private List<String> menus;
}
