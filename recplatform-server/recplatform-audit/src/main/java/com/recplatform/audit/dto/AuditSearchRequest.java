package com.recplatform.audit.dto;

import com.recplatform.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Audit log search request DTO.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditSearchRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String action;

    private String resource;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String result;
}
