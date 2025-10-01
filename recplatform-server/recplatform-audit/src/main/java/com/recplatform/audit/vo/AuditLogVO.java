package com.recplatform.audit.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit log view object.
 */
@Data
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

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

    private LocalDateTime createTime;
}
