package com.recplatform.common.enums;

/**
 * Solver status enumeration.
 */
public enum SolverStatus {

    PENDING,
    QUEUED,
    RUNNING,
    OPTIMAL,
    FEASIBLE,
    INFEASIBLE,
    UNBOUNDED,
    TIMEOUT,
    ERROR,
    CANCELLED
}
