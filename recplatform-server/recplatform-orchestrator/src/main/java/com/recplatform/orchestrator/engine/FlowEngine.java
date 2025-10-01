package com.recplatform.orchestrator.engine;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.common.enums.FlowStatus;
import com.recplatform.orchestrator.engine.nodes.NodeExecutor;
import com.recplatform.orchestrator.flow.dto.FlowDefinition;
import com.recplatform.orchestrator.flow.dto.FlowEdge;
import com.recplatform.orchestrator.flow.dto.FlowNode;
import com.recplatform.orchestrator.flow.vo.NodeExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Core flow execution engine.
 * Executes a FlowDefinition by walking the DAG in topological order,
 * handling parallel branches, decision gates, loops, and retry logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEngine {

    private final DagValidator dagValidator;
    private final NodeExecutorRegistry executorRegistry;

    /** Track running contexts for cancellation support. */
    private final ConcurrentHashMap<Long, FlowContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * Execute a flow definition with the given input parameters.
     *
     * @param definition   the flow definition to execute
     * @param inputParams  input parameters for the flow
     * @param executionId  the execution record ID
     * @return the execution result
     */
    public FlowExecutionResult execute(FlowDefinition definition, Map<String, Object> inputParams, Long executionId) {
        log.info("Starting flow execution: executionId={}", executionId);

        // Step 1: Validate DAG
        ValidationResult validation = dagValidator.validate(definition);
        if (!validation.isValid()) {
            return FlowExecutionResult.fail("流程结构校验失败：" + String.join("；", validation.getErrors()),
                    Collections.emptyList());
        }

        // Step 2: Build execution context
        FlowContext context = new FlowContext(executionId, null);
        if (inputParams != null) {
            inputParams.forEach(context::setGlobalVariable);
        }
        if (definition.getGlobalParams() != null) {
            definition.getGlobalParams().forEach(context::setGlobalVariable);
        }
        activeContexts.put(executionId, context);

        // Step 3: Build node map and adjacency lists
        Map<String, FlowNode> nodeMap = definition.getNodes().stream()
                .collect(Collectors.toMap(FlowNode::getId, n -> n, (a, b) -> a));

        Map<String, List<FlowEdge>> outgoingEdges = new HashMap<>();
        Map<String, List<FlowEdge>> incomingEdges = new HashMap<>();
        for (FlowNode node : definition.getNodes()) {
            outgoingEdges.put(node.getId(), new ArrayList<>());
            incomingEdges.put(node.getId(), new ArrayList<>());
        }
        if (definition.getEdges() != null) {
            for (FlowEdge edge : definition.getEdges()) {
                outgoingEdges.get(edge.getSource()).add(edge);
                incomingEdges.get(edge.getTarget()).add(edge);
            }
        }

        // Track node execution statuses
        List<NodeExecutionStatus> nodeExecutions = Collections.synchronizedList(new ArrayList<>());

        try {
            // Step 4: Find START node and begin execution
            FlowNode startNode = definition.getNodes().stream()
                    .filter(n -> n.getType() == FlowNodeType.START)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No START node found"));

            // Execute from START node following the DAG
            executeNodeChain(startNode, nodeMap, outgoingEdges, context, nodeExecutions);

            // Collect output from END node(s)
            Map<String, Object> output = new HashMap<>();
            for (FlowNode node : definition.getNodes()) {
                if (node.getType() == FlowNodeType.END) {
                    Object endOutput = context.getNodeOutput(node.getId());
                    if (endOutput instanceof Map) {
                        output.putAll((Map<String, Object>) endOutput);
                    }
                }
            }

            log.info("Flow execution completed successfully: executionId={}", executionId);
            return FlowExecutionResult.success(output, new ArrayList<>(nodeExecutions));

        } catch (Exception e) {
            log.error("Flow execution failed: executionId={}", executionId, e);
            return FlowExecutionResult.fail(e.getMessage(), new ArrayList<>(nodeExecutions));
        } finally {
            activeContexts.remove(executionId);
        }
    }

    /**
     * Cancel a running flow execution.
     */
    public void cancel(Long executionId) {
        FlowContext context = activeContexts.get(executionId);
        if (context != null) {
            log.info("Cancelling flow execution: executionId={}", executionId);
            context.cancel();
        }
    }

    /**
     * Execute a chain of nodes starting from the given node.
     */
    private void executeNodeChain(FlowNode startNode, Map<String, FlowNode> nodeMap,
                                  Map<String, List<FlowEdge>> outgoingEdges,
                                  FlowContext context, List<NodeExecutionStatus> nodeExecutions) {
        // Use a queue for BFS-like execution, but respecting DAG order
        Deque<String> executionQueue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        executionQueue.add(startNode.getId());

        while (!executionQueue.isEmpty()) {
            if (context.isCancelled()) {
                log.info("Flow execution cancelled, stopping node chain");
                return;
            }

            String currentNodeId = executionQueue.poll();
            if (visited.contains(currentNodeId)) {
                continue;
            }
            visited.add(currentNodeId);

            FlowNode currentNode = nodeMap.get(currentNodeId);
            if (currentNode == null) {
                log.warn("Node not found: {}", currentNodeId);
                continue;
            }

            context.setCurrentNodeId(currentNodeId);
            log.debug("Executing node: id={}, type={}, name={}", currentNodeId, currentNode.getType(), currentNode.getName());

            // Execute the node with retry logic
            NodeExecutionResult result = executeWithRetry(currentNode, context);

            // Record node execution status
            nodeExecutions.add(NodeExecutionStatus.builder()
                    .nodeId(currentNodeId)
                    .nodeName(currentNode.getName())
                    .nodeType(currentNode.getType())
                    .status(result.isSuccess() ? FlowStatus.COMPLETED : FlowStatus.FAILED)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .output(result.getOutput())
                    .error(result.getError())
                    .build());

            if (!result.isSuccess()) {
                log.error("Node execution failed: nodeId={}, error={}", currentNodeId, result.getError());
                throw new RuntimeException("节点「" + currentNode.getName() + "」执行失败：" + result.getError());
            }

            // Store output in context
            if (result.getOutput() != null) {
                context.setNodeOutput(currentNodeId, result.getOutput());
            }

            // Determine next nodes based on current node type
            List<FlowEdge> edges = outgoingEdges.getOrDefault(currentNodeId, Collections.emptyList());

            if (currentNode.getType() == FlowNodeType.PARALLEL_FORK) {
                // Execute all parallel branches concurrently
                executeParallelBranches(edges, nodeMap, outgoingEdges, context, nodeExecutions);
                visited.addAll(edges.stream().map(FlowEdge::getTarget).collect(Collectors.toList()));
                // After parallel fork, continue with PARALLEL_JOIN target
                for (FlowEdge edge : edges) {
                    FlowNode targetNode = nodeMap.get(edge.getTarget());
                    if (targetNode != null && targetNode.getType() == FlowNodeType.PARALLEL_JOIN) {
                        executionQueue.add(targetNode.getId());
                    }
                }
            } else if (currentNode.getType() == FlowNodeType.DECISION_GATE) {
                // Follow the chosen path
                String nextNodeId = result.getNextNodeId();
                if (nextNodeId != null) {
                    executionQueue.add(nextNodeId);
                } else {
                    // Default: follow first edge
                    if (!edges.isEmpty()) {
                        executionQueue.add(edges.get(0).getTarget());
                    }
                }
            } else if (currentNode.getType() == FlowNodeType.LOOP) {
                // Loop: re-execute if condition is still true
                // For simplicity, the LoopExecutor handles iteration internally
                // We just follow the edges after the loop completes
                for (FlowEdge edge : edges) {
                    executionQueue.add(edge.getTarget());
                }
            } else {
                // Default: follow all outgoing edges
                for (FlowEdge edge : edges) {
                    executionQueue.add(edge.getTarget());
                }
            }
        }
    }

    /**
     * Execute parallel branches concurrently using CompletableFuture.
     */
    private void executeParallelBranches(List<FlowEdge> edges, Map<String, FlowNode> nodeMap,
                                          Map<String, List<FlowEdge>> outgoingEdges,
                                          FlowContext context, List<NodeExecutionStatus> nodeExecutions) {
        if (edges.isEmpty()) {
            return;
        }

        log.debug("Executing {} parallel branches", edges.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (FlowEdge edge : edges) {
            FlowNode branchStart = nodeMap.get(edge.getTarget());
            if (branchStart == null) {
                continue;
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    executeNodeChain(branchStart, nodeMap, outgoingEdges, context, nodeExecutions);
                } catch (Exception e) {
                    log.error("Parallel branch execution failed: nodeId={}", edge.getTarget(), e);
                    throw new CompletionException(e);
                }
            });
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new RuntimeException("Parallel branch execution failed", e.getCause());
        }
    }

    /**
     * Execute a node with retry logic.
     */
    private NodeExecutionResult executeWithRetry(FlowNode node, FlowContext context) {
        int maxRetries = 0;
        long retryDelay = 1000;
        if (node.getRetryConfig() != null) {
            maxRetries = node.getRetryConfig().getMaxRetries();
            retryDelay = node.getRetryConfig().getRetryDelay();
        }

        NodeExecutionResult result = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (context.isCancelled()) {
                return NodeExecutionResult.fail("执行已取消");
            }

            try {
                if (attempt > 0) {
                    log.info("Retrying node: id={}, attempt={}/{}", node.getId(), attempt, maxRetries);
                    Thread.sleep(retryDelay);
                }

                // Apply timeout if configured
                if (node.getTimeout() != null && node.getTimeout() > 0) {
                    result = executeWithTimeout(node, context);
                } else {
                    NodeExecutor executor = executorRegistry.getExecutor(node.getType());
                    result = executor.execute(node, context);
                }

                if (result.isSuccess()) {
                    return result;
                }

                lastException = new RuntimeException(result.getError());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return NodeExecutionResult.fail("执行被中断");
            } catch (Exception e) {
                lastException = e;
                log.warn("Node execution attempt {} failed: id={}, error={}", attempt, node.getId(), e.getMessage());
            }
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error after retries";
        return NodeExecutionResult.fail(errorMsg);
    }

    /**
     * Execute a node with a timeout.
     */
    private NodeExecutionResult executeWithTimeout(FlowNode node, FlowContext context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<NodeExecutionResult> future = executor.submit(() -> {
                NodeExecutor nodeExecutor = executorRegistry.getExecutor(node.getType());
                return nodeExecutor.execute(node, context);
            });
            return future.get(node.getTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return NodeExecutionResult.fail("节点执行超时（超过 " + node.getTimeout() + " 秒）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NodeExecutionResult.fail("节点执行被中断");
        } catch (ExecutionException e) {
            return NodeExecutionResult.fail("节点执行失败："  + e.getCause().getMessage());
        } finally {
            executor.shutdownNow();
        }
    }
}
