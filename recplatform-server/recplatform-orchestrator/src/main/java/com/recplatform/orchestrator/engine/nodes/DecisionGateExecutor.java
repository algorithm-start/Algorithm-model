package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Executor for DECISION_GATE nodes.
 * Evaluates a SpEL condition against the flow context and returns
 * which outgoing edge (next node) to follow.
 */
@Slf4j
@Component
public class DecisionGateExecutor implements NodeExecutor {

    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing DECISION_GATE node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("决策网关节点缺少配置");
        }

        String condition = (String) config.get("condition");
        if (condition == null || condition.isEmpty()) {
            return NodeExecutionResult.fail("决策网关节点缺少 condition 条件配置");
        }

        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            // Expose global variables as root object
            evalContext.setVariables(context.getGlobalVariables());
            // Also expose node outputs
            evalContext.setVariable("nodeOutputs", context.getNodeOutputs());

            Expression expression = spelParser.parseExpression(condition);
            Object result = expression.getValue(evalContext);

            String nextNodeId = null;
            if (result instanceof String) {
                nextNodeId = (String) result;
            } else if (result instanceof Boolean && (Boolean) result) {
                nextNodeId = (String) config.get("trueBranch");
            } else if (result instanceof Boolean && !(Boolean) result) {
                nextNodeId = (String) config.get("falseBranch");
            }

            Map<String, Object> output = new HashMap<>();
            output.put("conditionResult", result);
            output.put("selectedPath", nextNodeId);

            log.debug("Decision gate result: condition={}, result={}, nextNode={}",
                    condition, result, nextNodeId);

            return NodeExecutionResult.success(output, nextNodeId);

        } catch (Exception e) {
            log.error("Decision gate evaluation failed: condition={}, error={}", condition, e.getMessage());
            return NodeExecutionResult.fail("决策网关条件计算失败："  + e.getMessage());
        }
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.DECISION_GATE;
    }
}
