package com.recplatform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Role creation request DTO.
 */
@Data
public class RoleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Role code cannot be blank")
    private String code;

    @NotBlank(message = "Role name cannot be blank")
    private String name;

    private String description;
}
