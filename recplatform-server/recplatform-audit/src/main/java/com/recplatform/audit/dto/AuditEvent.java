package com.recplatform.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Audit event DTO for internal logging.
 */
@Data
@Builder
public class AuditEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String action;

    private String resource;

    private String resourceId;

    private String detail;

    private String result;

    private String errorMessage;

    private String ip;

    private String userAgent;

    private Long duration;

    private String traceId;
}
