package com.recplatform.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Role assignment request DTO.
 */
@Data
public class RoleAssignRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Role IDs cannot be null")
    private List<Long> roleIds;
}
