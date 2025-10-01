package com.recplatform.audit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Alert rule creation request DTO.
 */
@Data
public class AlertRuleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Rule name cannot be blank")
    private String name;

    private String description;

    @NotBlank(message = "Condition cannot be blank")
    private String condition;

    private String severity;

    private String notification;
}
