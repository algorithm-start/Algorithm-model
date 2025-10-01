package com.recplatform.solver.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Problem definition DTO containing variables, constraints, and objective.
 */
@Data
public class ProblemDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<VariableDef> variables;

    private List<ConstraintDef> constraints;

    private ObjectiveDef objective;

    private Map<String, Object> metadata;

    /**
     * Variable definition.
     */
    @Data
    public static class VariableDef implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        /**
         * CONTINUOUS, INTEGER, or BINARY.
         */
        private String type;

        private Double lowerBound;

        private Double upperBound;
    }

    /**
     * Constraint definition.
     */
    @Data
    public static class ConstraintDef implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private String expression;

        /**
         * LEQ, GEQ, or EQ.
         */
        private String type;

        private Double rhs;
    }

    /**
     * Objective definition.
     */
    @Data
    public static class ObjectiveDef implements Serializable {

        private static final long serialVersionUID = 1L;

        private String expression;

        /**
         * MINIMIZE or MAXIMIZE.
         */
        private String sense;
    }
}
