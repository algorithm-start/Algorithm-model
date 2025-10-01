package com.recplatform.audit.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Audit statistics view object.
 */
@Data
@Builder
public class AuditStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalCount;

    private Map<String, Long> countByAction;

    private Map<String, Long> countByResource;

    private Map<String, Long> countByResult;

    /**
     * Timeline of daily counts.
     */
    private List<DailyCount> timeline;

    @Data
    @Builder
    public static class DailyCount implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private long count;
    }
}
