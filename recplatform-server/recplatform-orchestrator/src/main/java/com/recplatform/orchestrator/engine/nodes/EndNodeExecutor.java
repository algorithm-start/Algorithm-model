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
 * Executor for END nodes. Collects final output from context.
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing END node: id={}", node.getId());
        // Collect all node outputs as the final result
        Map<String, Object> output = new HashMap<>();
        context.getNodeOutputs().forEach((key, value) -> {
            if (value instanceof Map) {
                output.putAll((Map<String, Object>) value);
            } else {
                output.put(key, value);
            }
        });
        return NodeExecutionResult.success(output);
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.END;
    }
}
