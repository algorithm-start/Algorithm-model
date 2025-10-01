package com.recplatform.solver.model.vo;

import com.recplatform.common.enums.AlgorithmType;
import com.recplatform.common.enums.SolverStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View object for solver job API responses.
 */
@Data
public class SolverJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String problemName;

    private String problemDescription;

    /**
     * JSON-serialized ProblemDefinition.
     */
    private String problemDefinition;

    private AlgorithmType algorithmType;

    /**
     * JSON-serialized SolverConfig.
     */
    private String solverConfig;

    private SolverStatus status;

    /**
     * JSON-serialized solver result.
     */
    private String result;

    private Double objectiveValue;

    private Long solveTimeMs;

    /**
     * Formatted solve time string (e.g., "1.23s").
     */
    private String solveTimeFormatted;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String createBy;

    /**
     * Human-readable status description.
     */
    private String statusDescription;
}
