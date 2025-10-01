package com.recplatform.orchestrator.engine;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of DAG validation.
 */
@Data
@Builder
public class ValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean valid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).errors(new ArrayList<>()).build();
    }

    public static ValidationResult fail(List<String> errors) {
        return ValidationResult.builder().valid(false).errors(errors).build();
    }

    public static ValidationResult fail(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return ValidationResult.builder().valid(false).errors(errors).build();
    }
}
