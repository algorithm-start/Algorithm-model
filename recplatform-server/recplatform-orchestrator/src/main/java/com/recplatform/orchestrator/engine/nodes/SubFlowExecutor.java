package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.FlowExecutionResult;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.FlowService;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import com.recplatform.orchestrator.flow.vo.FlowExecutionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for SUB_FLOW nodes.
 * Invokes another flow by ID, mapping inputs and outputs.
 */
@Slf4j
@Component
public class SubFlowExecutor implements NodeExecutor {

    private final FlowService flowService;

    public SubFlowExecutor(@Lazy FlowService flowService) {
        this.flowService = flowService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing SUB_FLOW node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("子流程节点缺少配置");
        }

        Object subFlowIdObj = config.get("subFlowId");
        if (subFlowIdObj == null) {
            return NodeExecutionResult.fail("子流程节点缺少 subFlowId 配置");
        }

        Long subFlowId = ((Number) subFlowIdObj).longValue();

        // Build input params for sub-flow from context
        Map<String, Object> subFlowInput = new HashMap<>();
        Map<String, String> inputMapping = (Map<String, String>) config.get("inputMapping");
        if (inputMapping != null) {
            inputMapping.forEach((targetField, sourceExpr) -> {
                Object value = context.getGlobalVariable(sourceExpr);
                if (value != null) {
                    subFlowInput.put(targetField, value);
                }
            });
        } else {
            // Default: pass all global variables
            subFlowInput.putAll(context.getGlobalVariables());
        }

        try {
            FlowExecutionVO executionVO = flowService.executeFlow(subFlowId, subFlowInput);

            Map<String, Object> output = new HashMap<>();
            output.put("subFlowExecutionId", executionVO.getId());
            output.put("subFlowStatus", executionVO.getStatus().name());

            if (executionVO.getOutputResult() != null) {
                output.putAll(executionVO.getOutputResult());
            }

            return NodeExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Sub-flow execution failed: subFlowId={}, error={}", subFlowId, e.getMessage());
            return NodeExecutionResult.fail("子流程执行失败："  + e.getMessage());
        }
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.SUB_FLOW;
    }
}
