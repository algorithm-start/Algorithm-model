package com.recplatform.data.pipeline.dto;

import com.recplatform.data.pipeline.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Pipeline execution response VO.
 */
@Data
@Builder
public class PipelineExecutionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pipelineId;
    private ExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, String> nodeStatuses;
    private String errorMessage;
    private Map<String, Object> metrics;
}
