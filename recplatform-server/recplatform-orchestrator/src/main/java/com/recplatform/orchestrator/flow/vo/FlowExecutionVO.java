package com.recplatform.orchestrator.flow.vo;

import com.recplatform.common.enums.FlowStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * View object for flow execution details with node-level status list.
 */
@Data
@Builder
public class FlowExecutionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long flowId;
    private String flowName;
    private Integer flowVersion;
    private FlowStatus status;
    private Map<String, Object> inputParams;
    private Map<String, Object> outputResult;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<NodeExecutionStatus> nodeExecutions;
    private String errorMessage;
    private String createBy;
}
