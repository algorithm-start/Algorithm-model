package com.recplatform.common.enums;

/**
 * Flow node type enumeration.
 */
public enum FlowNodeType {

    SERVICE_CALL,
    SOLVER_INVOKE,
    DATA_TRANSFORM,
    DECISION_GATE,
    LOOP,
    SUB_FLOW,
    SCRIPT,
    START,
    END,
    PARALLEL_FORK,
    PARALLEL_JOIN
}
