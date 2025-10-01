package com.recplatform.solver.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.enums.AlgorithmType;
import com.recplatform.common.enums.SolverStatus;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Solver job entity representing a submitted optimization problem.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("solver_job")
public class SolverJobEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @com.baomidou.mybatisplus.annotation.TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;

    private String problemName;

    private String problemDescription;

    /**
     * JSON-serialized ProblemDefinition.
     */
    private String problemDefinition;

    /**
     * Algorithm type used for solving.
     */
    private AlgorithmType algorithmType;

    /**
     * JSON-serialized SolverConfig.
     */
    private String solverConfig;

    /**
     * Current status of the solver job.
     */
    private SolverStatus status;

    /**
     * JSON-serialized solver result.
     */
    private String result;

    /**
     * Optimal objective function value.
     */
    private Double objectiveValue;

    /**
     * Solve time in milliseconds.
     */
    private Long solveTimeMs;

    /**
     * Error message if solve failed.
     */
    private String errorMessage;
}
