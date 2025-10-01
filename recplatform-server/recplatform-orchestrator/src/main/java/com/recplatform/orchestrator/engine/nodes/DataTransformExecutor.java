package com.recplatform.orchestrator.engine.nodes;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.FlowContext;
import com.recplatform.orchestrator.engine.NodeExecutionResult;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Executor for DATA_TRANSFORM nodes.
 * Transforms data using configured operations: map, filter, aggregate.
 */
@Slf4j
@Component
public class DataTransformExecutor implements NodeExecutor {

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(FlowNode node, FlowContext context) {
        log.debug("Executing DATA_TRANSFORM node: id={}, name={}", node.getId(), node.getName());

        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeExecutionResult.fail("数据转换节点缺少配置");
        }

        String operation = (String) config.get("operation");
        if (operation == null) {
            return NodeExecutionResult.fail("数据转换节点缺少 operation 操作配置");
        }

        // Get input data from context
        String inputField = (String) config.getOrDefault("inputField", "data");
        Object inputData = context.getGlobalVariable(inputField);

        try {
            Map<String, Object> output = new HashMap<>();
            switch (operation.toLowerCase()) {
                case "map":
                    output.put("result", transformMap(inputData, config));
                    break;
                case "filter":
                    output.put("result", transformFilter(inputData, config));
                    break;
                case "aggregate":
                    output.put("result", transformAggregate(inputData, config));
                    break;
                default:
                    return NodeExecutionResult.fail("未知的数据转换操作："  + operation);
            }
            return NodeExecutionResult.success(output);
        } catch (Exception e) {
            log.error("Data transform failed: node={}, error={}", node.getId(), e.getMessage());
            return NodeExecutionResult.fail("数据转换失败："  + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> transformMap(Object inputData, Map<String, Object> config) {
        List<Map<String, Object>> items = toListOfMaps(inputData);
        String expression = (String) config.getOrDefault("expression", "");
        List<String> fields = (List<String>) config.get("fields");

        return items.stream().map(item -> {
            Map<String, Object> mapped = new HashMap<>();
            if (fields != null) {
                for (String field : fields) {
                    if (item.containsKey(field)) {
                        mapped.put(field, item.get(field));
                    }
                }
            } else {
                mapped.putAll(item);
            }
            return mapped;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> transformFilter(Object inputData, Map<String, Object> config) {
        List<Map<String, Object>> items = toListOfMaps(inputData);
        String expression = (String) config.getOrDefault("expression", "");
        String filterField = (String) config.get("field");
        Object filterValue = config.get("value");

        if (filterField == null) {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    Object val = item.get(filterField);
                    if (filterValue != null) {
                        return filterValue.equals(val);
                    }
                    return val != null;
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Object transformAggregate(Object inputData, Map<String, Object> config) {
        List<Map<String, Object>> items = toListOfMaps(inputData);
        String aggField = (String) config.get("field");
        String aggOp = (String) config.getOrDefault("aggregateOp", "count");

        if ("count".equals(aggOp)) {
            Map<String, Object> result = new HashMap<>();
            result.put("count", items.size());
            return result;
        }

        if (aggField == null) {
            return Collections.singletonMap("count", items.size());
        }

        List<Double> values = items.stream()
                .filter(item -> item.get(aggField) instanceof Number)
                .map(item -> ((Number) item.get(aggField)).doubleValue())
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        switch (aggOp) {
            case "sum":
                result.put("sum", values.stream().mapToDouble(Double::doubleValue).sum());
                break;
            case "avg":
                result.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                break;
            case "min":
                result.put("min", values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
                break;
            case "max":
                result.put("max", values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
                break;
            default:
                result.put("count", items.size());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toListOfMaps(Object data) {
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return Collections.emptyList();
    }

    @Override
    public FlowNodeType supportedType() {
        return FlowNodeType.DATA_TRANSFORM;
    }
}
