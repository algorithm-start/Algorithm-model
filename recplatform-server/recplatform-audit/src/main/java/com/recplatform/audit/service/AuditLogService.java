package com.recplatform.audit.service;

import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.dto.AuditSearchRequest;
import com.recplatform.audit.vo.AuditLogVO;
import com.recplatform.audit.vo.AuditStats;
import com.recplatform.common.model.PageResult;

import java.time.LocalDateTime;

/**
 * Audit log service.
 */
public interface AuditLogService {

    /**
     * Save an audit log entry.
     */
    void log(AuditEvent event);

    /**
     * Search audit logs with pagination and filters.
     */
    PageResult<AuditLogVO> search(AuditSearchRequest request);

    /**
     * Get audit statistics for a time range.
     */
    AuditStats getStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Export audit logs as CSV byte array.
     */
    byte[] export(AuditSearchRequest request);
}
