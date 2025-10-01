package com.recplatform.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Unified result code enumeration.
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 2xx Success
    SUCCESS(200, "Success"),

    // 4xx Client Errors
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    // 5xx Server Errors
    INTERNAL_ERROR(500, "Internal Server Error"),

    // 1xxx Solver Errors
    SOLVER_TIMEOUT(1001, "Solver Timeout"),
    SOLVER_INFEASIBLE(1002, "Solver Infeasible"),
    SOLVER_UNBOUNDED(1003, "Solver Unbounded"),
    SOLVER_ERROR(1004, "Solver Error"),
    SOLVER_CANCELLED(1005, "Solver Cancelled"),

    // 2xxx Data Errors
    DATA_SOURCE_UNREACHABLE(2001, "Data Source Unreachable"),
    DATA_PIPELINE_FAILED(2002, "Data Pipeline Failed"),
    DATA_TRANSFORM_ERROR(2003, "Data Transform Error"),
    DATA_API_ERROR(2004, "Data API Error"),
    DATA_QUERY_FAILED(2005, "Query Execution Failed"),

    // 3xxx Flow Errors
    FLOW_EXECUTION_FAILED(3001, "Flow Execution Failed"),
    FLOW_NODE_TIMEOUT(3002, "Flow Node Timeout"),
    FLOW_CYCLE_DETECTED(3003, "Flow Cycle Detected"),
    FLOW_INVALID_CONFIG(3004, "Flow Invalid Config"),

    // 4xxx Auth Errors
    AUTH_FAILED(4001, "Authentication Failed"),
    TOKEN_EXPIRED(4002, "Token Expired"),
    PERMISSION_DENIED(4003, "Permission Denied"),
    USER_DISABLED(4004, "User Disabled"),

    // 5xxx Audit Errors
    AUDIT_LOG_ERROR(5001, "Audit Log Error");

    private final int code;
    private final String message;
}
