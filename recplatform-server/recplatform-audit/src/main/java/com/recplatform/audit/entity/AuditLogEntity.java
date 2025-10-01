package com.recplatform.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Audit log entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLogEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    /**
     * Action type: CREATE, UPDATE, DELETE, LOGIN, EXECUTE, EXPORT
     */
    private String action;

    /**
     * Resource type: e.g., "solver_problem", "data_source"
     */
    private String resource;

    private String resourceId;

    /**
     * JSON detail - request body/params
     */
    private String detail;

    /**
     * Result: SUCCESS, FAILURE
     */
    private String result;

    private String errorMessage;

    private String ip;

    private String userAgent;

    /**
     * Duration in milliseconds.
     */
    private Long duration;

    private String traceId;
}
