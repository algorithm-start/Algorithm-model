package com.recplatform.orchestrator.engine;

import com.recplatform.common.enums.FlowStatus;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds execution state for a running flow.
 */
@Data
public class FlowContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long executionId;
    private Long flowId;
    private Map<String, Object> globalVariables;
    private Map<String, Object> nodeOutputs;
    private volatile FlowStatus status;
    private volatile String currentNodeId;
    private long startTime;
    private volatile boolean cancelled;

    public FlowContext(Long executionId, Long flowId) {
        this.executionId = executionId;
        this.flowId = flowId;
        this.globalVariables = new ConcurrentHashMap<>();
        this.nodeOutputs = new ConcurrentHashMap<>();
        this.status = FlowStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
        this.cancelled = false;
    }

    /**
     * Store a node's output.
     */
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * Get a node's output.
     */
    @SuppressWarnings("unchecked")
    public <T> T getNodeOutput(String nodeId) {
        return (T) nodeOutputs.get(nodeId);
    }

    /**
     * Set a global variable.
     */
    public void setGlobalVariable(String key, Object value) {
        globalVariables.put(key, value);
    }

    /**
     * Get a global variable.
     */
    @SuppressWarnings("unchecked")
    public <T> T getGlobalVariable(String key) {
        return (T) globalVariables.get(key);
    }

    /**
     * Check if execution has been cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Mark the execution as cancelled.
     */
    public void cancel() {
        this.cancelled = true;
        this.status = FlowStatus.CANCELLED;
    }
}
