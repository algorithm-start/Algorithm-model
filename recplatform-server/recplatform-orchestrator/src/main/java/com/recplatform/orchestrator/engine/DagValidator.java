package com.recplatform.orchestrator.engine;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import com.recplatform.orchestrator.flow.dto.FlowEdge;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates a flow definition DAG structure.
 * Checks: exactly one START, at least one END, no cycles (Kahn's algorithm),
 * all nodes reachable from START, all edge references valid.
 */
@Slf4j
@Component
public class DagValidator {

    /**
     * Validate the given flow definition.
     */
    public ValidationResult validate(FlowDefinition definition) {
        if (definition == null) {
            return ValidationResult.fail("工作流定义为空");
        }

        List<String> errors = new ArrayList<>();

        List<FlowNode> nodes = definition.getNodes();
        List<FlowEdge> edges = definition.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            return ValidationResult.fail("工作流至少需要一个节点");
        }

        // Build node map
        Map<String, FlowNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(FlowNode::getId, n -> n, (a, b) -> a));

        // Check for duplicate node IDs
        Set<String> nodeIds = new HashSet<>();
        for (FlowNode node : nodes) {
            if (!nodeIds.add(node.getId())) {
                errors.add("存在重复的节点 ID：" + node.getId());
            }
        }

        // Check: exactly one START node
        List<FlowNode> startNodes = nodes.stream()
                .filter(n -> n.getType() == FlowNodeType.START)
                .collect(Collectors.toList());
        if (startNodes.isEmpty()) {
            errors.add("工作流必须有且仅有一个「开始」节点");
        } else if (startNodes.size() > 1) {
            errors.add("工作流只能有一个「开始」节点，当前有 " + startNodes.size() + " 个");
        }

        // Check: at least one END node
        List<FlowNode> endNodes = nodes.stream()
                .filter(n -> n.getType() == FlowNodeType.END)
                .collect(Collectors.toList());
        if (endNodes.isEmpty()) {
            errors.add("工作流至少需要一个「结束」节点");
        }

        // Validate each node's required configuration
        for (FlowNode node : nodes) {
            errors.addAll(validateNodeConfig(node));
        }

        // Validate edge references
        if (edges != null) {
            for (FlowEdge edge : edges) {
                if (!nodeMap.containsKey(edge.getSource())) {
                    errors.add("连线引用了不存在的起始节点：" + edge.getSource());
                }
                if (!nodeMap.containsKey(edge.getTarget())) {
                    errors.add("连线引用了不存在的目标节点：" + edge.getTarget());
                }
                if (edge.getSource() != null && edge.getSource().equals(edge.getTarget())) {
                    errors.add("节点存在指向自身的连线：" + nodeLabel(nodeMap.get(edge.getSource())));
                }
            }
        }

        // Check for cycles using Kahn's algorithm
        if (errors.isEmpty()) {
            String cycleError = detectCycle(nodes, edges);
            if (cycleError != null) {
                errors.add(cycleError);
            }
        }

        // Check all nodes reachable from START
        if (errors.isEmpty() && startNodes.size() == 1) {
            String reachabilityError = checkReachability(startNodes.get(0).getId(), nodes, edges);
            if (reachabilityError != null) {
                errors.add(reachabilityError);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("DAG validation failed: {}", errors);
            return ValidationResult.fail(errors);
        }

        return ValidationResult.ok();
    }

    /**
     * Detect cycles using Kahn's algorithm (topological sort).
     */
    private String detectCycle(List<FlowNode> nodes, List<FlowEdge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (FlowNode node : nodes) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }

        if (edges != null) {
            for (FlowEdge edge : edges) {
                adjacency.get(edge.getSource()).add(edge.getTarget());
                inDegree.merge(edge.getTarget(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int processedCount = 0;
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            processedCount++;
            for (String neighbor : adjacency.get(nodeId)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (processedCount != nodes.size()) {
            return "工作流中存在环路，无法按拓扑顺序执行所有节点，请检查节点连线。";
        }
        return null;
    }

    /**
     * Validates that a node has all required configuration for its type, so that
     * missing parameters are surfaced at publish time rather than at runtime.
     */
    private List<String> validateNodeConfig(FlowNode node) {
        List<String> errors = new ArrayList<>();
        if (node.getType() == null) {
            errors.add("节点「" + nodeLabel(node) + "」缺少节点类型");
            return errors;
        }
        Map<String, Object> config = node.getConfig();
        switch (node.getType()) {
            case DATA_TRANSFORM:
                requireConfig(node, config, "operation", "数据转换操作", errors);
                break;
            case SERVICE_CALL:
                requireConfig(node, config, "url", "服务地址", errors);
                break;
            case SOLVER_INVOKE:
                requireConfig(node, config, "problemId", "求解问题 ID", errors);
                break;
            case DECISION_GATE:
                requireConfig(node, config, "condition", "判断条件", errors);
                break;
            case SCRIPT:
                requireConfig(node, config, "script", "脚本内容", errors);
                break;
            case SUB_FLOW:
                requireConfig(node, config, "subFlowId", "子流程 ID", errors);
                break;
            case LOOP:
                if (config == null
                        || (isBlank(config.get("collectionExpression")) && isBlank(config.get("condition")))) {
                    errors.add("节点「" + nodeLabel(node) + "」缺少循环配置（需配置 集合表达式 或 循环条件）");
                }
                break;
            default:
                break;
        }
        return errors;
    }

    private void requireConfig(FlowNode node, Map<String, Object> config, String key,
                               String label, List<String> errors) {
        if (config == null || isBlank(config.get(key))) {
            errors.add("节点「" + nodeLabel(node) + "」缺少必填配置：" + label + "（" + key + "）");
        }
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private String nodeLabel(FlowNode node) {
        if (node == null) {
            return "未知节点";
        }
        if (node.getName() != null && !node.getName().isBlank()) {
            return node.getName();
        }
        return node.getId();
    }

    /**
     * Check that all nodes are reachable from the START node via BFS.
     */
    private String checkReachability(String startNodeId, List<FlowNode> nodes, List<FlowEdge> edges) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (FlowNode node : nodes) {
            adjacency.put(node.getId(), new ArrayList<>());
        }
        if (edges != null) {
            for (FlowEdge edge : edges) {
                adjacency.get(edge.getSource()).add(edge.getTarget());
            }
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startNodeId);
        visited.add(startNodeId);

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            for (String neighbor : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        Set<String> unreachable = nodes.stream()
                .map(FlowNode::getId)
                .filter(id -> !visited.contains(id))
                .collect(Collectors.toSet());

        if (!unreachable.isEmpty()) {
            return "存在从「开始」节点无法到达的节点（请检查连线是否完整）：" + unreachable;
        }
        return null;
    }
}
