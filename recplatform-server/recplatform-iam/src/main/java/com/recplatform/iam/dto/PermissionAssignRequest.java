package com.recplatform.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Permission assignment request DTO.
 */
@Data
public class PermissionAssignRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Permission codes cannot be null")
    private List<String> permissionCodes;
}
