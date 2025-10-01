package com.recplatform.solver.model.dto;

import com.recplatform.common.enums.AlgorithmType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Solver configuration DTO.
 */
@Data
public class SolverConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Algorithm type to use for solving.
     */
    private AlgorithmType algorithmType;

    /**
     * Time limit in seconds.
     */
    private Integer timeLimit;

    /**
     * MIP gap tolerance.
     */
    private Double gapTolerance;

    /**
     * Maximum number of iterations.
     */
    private Integer maxIterations;

    /**
     * Number of threads (0 = auto).
     */
    private Integer threads;

    /**
     * Custom solver-specific parameters.
     */
    private Map<String, Object> customParams;
}
