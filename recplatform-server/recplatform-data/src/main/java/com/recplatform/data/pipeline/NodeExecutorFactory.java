package com.recplatform.data.pipeline;

import com.recplatform.data.pipeline.node.AbstractNodeExecutor;
import com.recplatform.data.pipeline.node.ExtractNodeExecutor;
import com.recplatform.data.pipeline.node.LoadNodeExecutor;
import com.recplatform.data.pipeline.node.TransformNodeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating appropriate node executor based on node type.
 */
@Component
@RequiredArgsConstructor
public class NodeExecutorFactory {

    private final ExtractNodeExecutor extractNodeExecutor;
    private final TransformNodeExecutor transformNodeExecutor;
    private final LoadNodeExecutor loadNodeExecutor;

    public AbstractNodeExecutor getExecutor(NodeType nodeType) {
        return switch (nodeType) {
            case EXTRACT -> extractNodeExecutor;
            case TRANSFORM -> transformNodeExecutor;
            case LOAD -> loadNodeExecutor;
        };
    }
}
