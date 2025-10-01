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
 * Executor for PARALLEL_FORK nodes.
 * The actual parallel execution is handled by the FlowEngine.
 * This executor simply passes context data through to downstream branches.
 */
@Slf4j
@Component
public class ParallelForkExecutor implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing PARALLEL_FORK node: id={}, name={}", node.getId(), node.getName());
        // The actual parallel branch execution is handled by FlowEngine.
        // This node simply passes through current context as output.
        Map<String, Object> output = new HashMap<>(context.getGlobalVariables());
        return NodeExecutionResult.success(output);
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.PARALLEL_FORK;
    }
}
