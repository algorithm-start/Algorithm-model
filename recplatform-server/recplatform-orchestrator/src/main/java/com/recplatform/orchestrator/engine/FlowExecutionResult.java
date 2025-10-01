package com.recplatform.orchestrator.engine;

import com.recplatform.orchestrator.flow.vo.NodeExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Result of a complete flow execution.
 */
@Data
@Builder
public class FlowExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private Map<String, Object> output;
    private List<NodeExecutionStatus> nodeExecutions;
    private String errorMessage;

    public static FlowExecutionResult success(Map<String, Object> output, List<NodeExecutionStatus> nodeExecutions) {
        return FlowExecutionResult.builder()
                .success(true)
                .output(output)
                .nodeExecutions(nodeExecutions)
                .build();
    }

    public static FlowExecutionResult fail(String errorMessage, List<NodeExecutionStatus> nodeExecutions) {
        return FlowExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .nodeExecutions(nodeExecutions)
                .build();
    }
}
