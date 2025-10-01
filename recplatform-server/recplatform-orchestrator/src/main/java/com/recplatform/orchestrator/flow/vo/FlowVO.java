package com.recplatform.orchestrator.flow.vo;

import com.recplatform.common.enums.FlowStatus;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View object for flow details including parsed node/edge counts.
 */
@Data
@Builder
public class FlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Integer version;
    private FlowDefinition definition;
    private FlowStatus status;
    private String category;
    private List<String> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;

    /** Parsed counts from definition. */
    private int nodeCount;
    private int edgeCount;
}
