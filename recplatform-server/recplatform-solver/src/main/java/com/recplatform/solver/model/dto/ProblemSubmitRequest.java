package com.recplatform.solver.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Request DTO for submitting a new optimization problem.
 */
@Data
public class ProblemSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Problem name is required")
    private String problemName;

    private String description;

    @Valid
    private ProblemDefinition problemDefinition;
}
