package com.recplatform.solver.model.vo;

import com.recplatform.solver.model.dto.ProblemDefinition;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Detail view object for a single example, including the full problem definition.
 * The {@code problem} field is shaped to match the frontend's expected structure
 * so it can populate the problem-creation form directly.
 */
@Data
@Builder
public class SolverExampleDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String type;

    private String description;

    private String recommendedAlgorithm;

    private String difficulty;

    /**
     * Nested problem payload consumed by the frontend form.
     */
    private ProblemPayload problem;

    @Data
    @Builder
    public static class ProblemPayload implements Serializable {

        private static final long serialVersionUID = 1L;

        private String problemName;

        private String description;

        private ProblemDefinition problemDefinition;
    }
}
