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

import java.util.*;

/**
 * Executor for LOOP nodes.
 * Iterates over a collection or until a condition is met.
 * Executes sub-nodes for each iteration.
 */
@Slf4j
@Component
public class LoopExecutor implements NodeExecutor {

    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing LOOP node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("循环节点缺少配置");
        }

        // Determine loop mode: collection-based or condition-based
        String collectionExpr = (String) config.get("collectionExpression");
        String conditionExpr = (String) config.get("condition");
        int maxIterations = config.containsKey("maxIterations")
                ? ((Number) config.get("maxIterations")).intValue() : 100;

        List<Map<String, Object>> iterationResults = new ArrayList<>();
        int iterationCount = 0;

        try {
            if (collectionExpr != null) {
                // Collection-based loop
                Object collection = evaluateExpression(collectionExpr, context);
                if (collection instanceof Iterable) {
                    for (Object item : (Iterable<?>) collection) {
                        if (context.isCancelled()) break;
                        if (iterationCount >= maxIterations) {
                            log.warn("Loop reached max iterations: {}", maxIterations);
                            break;
                        }
                        context.setGlobalVariable("loopItem", item);
                        context.setGlobalVariable("loopIndex", iterationCount);
                        iterationResults.add(Map.of("index", iterationCount, "item", item));
                        iterationCount++;
                    }
                }
            } else if (conditionExpr != null) {
                // Condition-based loop
                while (iterationCount < maxIterations) {
                    if (context.isCancelled()) break;
                    Object condResult = evaluateExpression(conditionExpr, context);
                    if (condResult instanceof Boolean && !(Boolean) condResult) {
                        break;
                    }
                    context.setGlobalVariable("loopIndex", iterationCount);
                    iterationResults.add(Map.of("index", iterationCount));
                    iterationCount++;
                }
            } else {
                return NodeExecutionResult.fail("循环节点必须配置 collectionExpression 或 condition");
            }

            Map<String, Object> output = new HashMap<>();
            output.put("iterationCount", iterationCount);
            output.put("results", iterationResults);
            return NodeExecutionResult.success(output);

        } catch (Exception e) {
            log.error("Loop execution failed: node={}, error={}", node.getId(), e.getMessage());
            return NodeExecutionResult.fail("循环执行失败："  + e.getMessage());
        }
    }

    private Object evaluateExpression(String expression, FlowContext context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariables(context.getGlobalVariables());
        Expression expr = spelParser.parseExpression(expression);
        return expr.getValue(evalContext);
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.LOOP;
    }
}
