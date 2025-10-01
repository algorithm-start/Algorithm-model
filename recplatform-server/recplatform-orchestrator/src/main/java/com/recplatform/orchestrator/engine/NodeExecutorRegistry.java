package com.recplatform.orchestrator.engine;

import com.recplatform.common.enums.FlowNodeType;
import com.recplatform.orchestrator.engine.nodes.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry that maps FlowNodeType to NodeExecutor implementations.
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final Map<FlowNodeType, NodeExecutor> executorMap = new EnumMap<>(FlowNodeType.class);

    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            executorMap.put(executor.supportedType(), executor);
            log.info("Registered node executor for type: {}", executor.supportedType());
        }
    }

    /**
     * Get the executor for the given node type.
     */
    public NodeExecutor getExecutor(FlowNodeType type) {
        NodeExecutor executor = executorMap.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for node type: " + type);
        }
        return executor;
    }

    /**
     * Check if an executor exists for the given node type.
     */
    public boolean hasExecutor(FlowNodeType type) {
        return executorMap.containsKey(type);
    }
}
