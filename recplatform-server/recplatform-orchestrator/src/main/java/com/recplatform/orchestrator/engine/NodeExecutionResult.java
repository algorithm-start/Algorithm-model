package com.recplatform.orchestrator.engine;

import com.recplatform.common.enums.FlowStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Result of executing a single node.
 */
@Data
@Builder
public class NodeExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private FlowStatus status;
    private Map<String, Object> output;
    private String error;
    private String nextNodeId;

    public static NodeExecutionResult success(Map<String, Object> output) {
        return NodeExecutionResult.builder()
                .success(true)
                .status(FlowStatus.COMPLETED)
                .output(output)
                .build();
    }

    public static NodeExecutionResult success(Map<String, Object> output, String nextNodeId) {
        return NodeExecutionResult.builder()
                .success(true)
                .status(FlowStatus.COMPLETED)
                .output(output)
                .nextNodeId(nextNodeId)
                .build();
    }

    public static NodeExecutionResult fail(String error) {
        return NodeExecutionResult.builder()
                .success(false)
                .status(FlowStatus.FAILED)
                .error(error)
                .build();
    }
}
