package com.recplatform.data.pipeline;

import com.recplatform.common.enums.PipelineStatus;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.data.pipeline.dto.PipelineDefinition;
import com.recplatform.data.pipeline.dto.PipelineEdge;
import com.recplatform.data.pipeline.dto.PipelineNode;
import com.recplatform.data.pipeline.node.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Pipeline execution engine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineExecutor {

    private final NodeExecutorFactory nodeExecutorFactory;
    private final PipelineExecutionMapper executionMapper;
    private final PipelineMapper pipelineMapper;

    private final Map<Long, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    /**
     * Execute a pipeline asynchronously.
     */
    @Async
    public void execute(PipelineEntity pipeline) {
        PipelineExecutionEntity execution = createExecution(pipeline);
        try {
            PipelineDefinition definition = JsonUtil.fromJson(pipeline.getDefinition(), PipelineDefinition.class);
            if (definition == null || definition.getNodes() == null || definition.getNodes().isEmpty()) {
                failExecution(execution, "Pipeline definition is empty");
                return;
            }

            List<PipelineNode> sortedNodes = topologicalSort(definition);
            Map<String, List<Map<String, Object>>> nodeOutputs = new ConcurrentHashMap<>();
            Map<String, String> nodeStatuses = new ConcurrentHashMap<>();

            for (PipelineNode node : sortedNodes) {
                if (Boolean.TRUE.equals(cancellationFlags.get(execution.getId()))) {
                    cancelExecution(execution, nodeStatuses);
                    return;
                }

                nodeStatuses.put(node.getId(), "RUNNING");
                updateNodeStatuses(execution, nodeStatuses);

                List<Map<String, Object>> inputData = gatherInputData(definition, node, nodeOutputs);
                AbstractNodeExecutor executor = nodeExecutorFactory.getExecutor(node.getType());

                try {
                    List<Map<String, Object>> output = executor.execute(node, inputData);
                    nodeOutputs.put(node.getId(), output);
                    nodeStatuses.put(node.getId(), "COMPLETED");
                } catch (Exception e) {
                    log.error("Node execution failed: nodeId={}, name={}", node.getId(), node.getName(), e);
                    nodeStatuses.put(node.getId(), "FAILED");
                    failExecution(execution, "Node '" + node.getName() + "' failed: " + e.getMessage());
                    updateNodeStatuses(execution, nodeStatuses);
                    return;
                }

                updateNodeStatuses(execution, nodeStatuses);
            }

            completeExecution(execution, nodeStatuses);
        } catch (Exception e) {
            log.error("Pipeline execution failed: pipelineId={}", pipeline.getId(), e);
            failExecution(execution, e.getMessage());
        }
    }

    /**
     * Cancel a running pipeline execution.
     */
    public void cancel(Long executionId) {
        cancellationFlags.put(executionId, true);
    }

    private PipelineExecutionEntity createExecution(PipelineEntity pipeline) {
        PipelineExecutionEntity execution = new PipelineExecutionEntity();
        execution.setPipelineId(pipeline.getId());
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        execution.setNodeStatuses("{}");
        execution.setMetrics("{}");
        executionMapper.insert(execution);

        pipeline.setStatus(PipelineStatus.RUNNING);
        pipelineMapper.updateById(pipeline);
        return execution;
    }

    private void completeExecution(PipelineExecutionEntity execution, Map<String, String> nodeStatuses) {
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setEndTime(LocalDateTime.now());
        execution.setNodeStatuses(JsonUtil.toJson(nodeStatuses));
        executionMapper.updateById(execution);

        PipelineEntity pipeline = pipelineMapper.selectById(execution.getPipelineId());
        if (pipeline != null) {
            pipeline.setStatus(PipelineStatus.COMPLETED);
            pipeline.setLastRunTime(LocalDateTime.now());
            pipelineMapper.updateById(pipeline);
        }
        log.info("Pipeline execution completed: executionId={}", execution.getId());
    }

    private void failExecution(PipelineExecutionEntity execution, String errorMessage) {
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setEndTime(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        executionMapper.updateById(execution);

        PipelineEntity pipeline = pipelineMapper.selectById(execution.getPipelineId());
        if (pipeline != null) {
            pipeline.setStatus(PipelineStatus.FAILED);
            pipeline.setLastRunTime(LocalDateTime.now());
            pipelineMapper.updateById(pipeline);
        }
        log.error("Pipeline execution failed: executionId={}, error={}", execution.getId(), errorMessage);
    }

    private void cancelExecution(PipelineExecutionEntity execution, Map<String, String> nodeStatuses) {
        execution.setStatus(ExecutionStatus.CANCELLED);
        execution.setEndTime(LocalDateTime.now());
        execution.setErrorMessage("Execution cancelled by user");
        execution.setNodeStatuses(JsonUtil.toJson(nodeStatuses));
        executionMapper.updateById(execution);

        PipelineEntity pipeline = pipelineMapper.selectById(execution.getPipelineId());
        if (pipeline != null) {
            pipeline.setStatus(PipelineStatus.CANCELLED);
            pipelineMapper.updateById(pipeline);
        }
        cancellationFlags.remove(execution.getId());
        log.info("Pipeline execution cancelled: executionId={}", execution.getId());
    }

    private void updateNodeStatuses(PipelineExecutionEntity execution, Map<String, String> nodeStatuses) {
        execution.setNodeStatuses(JsonUtil.toJson(nodeStatuses));
        executionMapper.updateById(execution);
    }

    private List<PipelineNode> topologicalSort(PipelineDefinition definition) {
        Map<String, PipelineNode> nodeMap = definition.getNodes().stream()
                .collect(Collectors.toMap(PipelineNode::getId, n -> n));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (PipelineNode node : definition.getNodes()) {
            inDegree.putIfAbsent(node.getId(), 0);
            adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        }

        for (PipelineEdge edge : definition.getEdges()) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
            inDegree.merge(edge.getTarget(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<PipelineNode> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sorted.add(nodeMap.get(nodeId));
            for (String neighbor : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != definition.getNodes().size()) {
            throw new BusinessException(ResultCode.DATA_PIPELINE_FAILED,
                    "Pipeline DAG contains a cycle");
        }

        return sorted;
    }

    private List<Map<String, Object>> gatherInputData(PipelineDefinition definition,
                                                      PipelineNode node,
                                                      Map<String, List<Map<String, Object>>> nodeOutputs) {
        List<Map<String, Object>> inputData = new ArrayList<>();
        for (PipelineEdge edge : definition.getEdges()) {
            if (edge.getTarget().equals(node.getId())) {
                List<Map<String, Object>> upstreamData = nodeOutputs.get(edge.getSource());
                if (upstreamData != null) {
                    inputData.addAll(upstreamData);
                }
            }
        }
        return inputData;
    }
}
