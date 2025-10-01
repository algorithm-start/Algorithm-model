package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for PARALLEL_JOIN nodes.
 * Waits for all parallel branches to complete (handled by FlowEngine),
 * then merges the outputs from all branches.
 */
@Slf4j
@Component
public class ParallelJoinExecutor implements NodeExecutor {

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing PARALLEL_JOIN node: id={}, name={}", node.getId(), node.getName());

        // Merge all node outputs into a single output map.
        // By the time this executor runs, all parallel branches have completed.
        Map<String, Object> mergedOutput = new HashMap<>();
        context.getNodeOutputs().forEach((nodeId, output) -> {
            if (output instanceof Map) {
                mergedOutput.putAll((Map<String, Object>) output);
            } else {
                mergedOutput.put(nodeId, output);
            }
        });

        return NodeExecutionResult.success(mergedOutput);
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.PARALLEL_JOIN;
    }
}
