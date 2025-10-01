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
 * Executor for START nodes. Pass-through, sets initial context.
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing START node: id={}", node.getId());
        // Pass through all global variables as the initial output
        Map<String, Object> output = new HashMap<>(context.getGlobalVariables());
        return NodeExecutionResult.success(output);
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.START;
    }
}
