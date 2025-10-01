package com.recplatform.data.pipeline.dto;

import com.recplatform.common.enums.PipelineStatus;
import com.recplatform.data.pipeline.PipelineMode;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Pipeline response VO.
 */
@Data
@Builder
public class PipelineVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private PipelineDefinition definition;
    private PipelineMode mode;
    private String schedule;
    private PipelineStatus status;
    private LocalDateTime lastRunTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
