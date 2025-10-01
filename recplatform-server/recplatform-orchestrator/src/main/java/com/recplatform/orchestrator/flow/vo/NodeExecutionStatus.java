package com.recplatform.orchestrator.flow.vo;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.common.enums.FlowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Status of a single node execution within a flow run.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nodeId;
    private String nodeName;
    private FlowNodeType nodeType;
    private FlowStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> output;
    private String error;
}
