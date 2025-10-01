package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;

/**
 * Interface for node executors that handle different flow node types.
 */
public interface NodeExecutor {

    /**
     * Execute the given node within the provided flow context.
     */
    NodeExecutionResult execute(FlowNode node, FlowContext context);

    /**
     * Returns the flow node type this executor supports.
     */
    FlowNodeType supportedType();
}
