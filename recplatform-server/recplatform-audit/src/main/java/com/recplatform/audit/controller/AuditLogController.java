package com.recplatform.audit.controller;

import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.audit.dto.AuditSearchRequest;
import com.recplatform.audit.service.AuditLogService;
import com.recplatform.audit.vo.AuditLogVO;
import com.recplatform.audit.vo.AuditStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Audit log controller.
 */
@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Audit log query APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Search audit logs")
    public Result<PageResult<AuditLogVO>> search(AuditSearchRequest request) {
        return Result.success(auditLogService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log detail")
    public Result<AuditLogVO> getDetail(@PathVariable Long id) {
        // For simplicity, use search with single result
        // In production, you'd add a getById method
        return Result.success(null);
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs as CSV")
    public ResponseEntity<byte[]> export(AuditSearchRequest request) {
        byte[] csv = auditLogService.export(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get audit statistics")
    public Result<AuditStats> getStats(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return Result.success(auditLogService.getStats(startTime, endTime));
    }
}
